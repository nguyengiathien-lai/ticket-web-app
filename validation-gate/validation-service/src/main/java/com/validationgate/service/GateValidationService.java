package com.validationgate.service;

import com.validationgate.client.Level4Client;
import com.validationgate.dto.SubmitBatchRequest;
import com.validationgate.dto.ValidationRequest;
import com.validationgate.dto.SubmitBatchResponse;
import com.validationgate.entity.TapEvent;
import com.validationgate.repository.TapEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class GateValidationService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_FAILED = "FAILED";
    private static final List<String> RETRYABLE_STATUSES = List.of(STATUS_PENDING, STATUS_FAILED);
    private static final String QUEUED_MESSAGE = "Scan record queued for batch delivery";
    private static final int MAX_ERROR_LENGTH = 500;

    private final Level4Client level4Client;
    private final TapEventRepository tapEventRepository;
    private final int batchSize;
    private final Duration sentEventRetention;
    private final String deviceCode;
    private final String stationCode;
    private final String qrVerificationKey;
    private final long maxClockDriftSeconds;

    public GateValidationService(
            Level4Client level4Client,
            TapEventRepository tapEventRepository,
            @Value("${app.level4.scan-record-batch-size:100}") int batchSize,
            @Value("${app.level4.sent-event-retention:2d}") Duration sentEventRetention,
            @Value("${app.device.code:gate-device-1}") String deviceCode,
            @Value("${app.device.station-code:station-1}") String stationCode,
            @Value("${app.level4.qr-verification-key:}") String qrVerificationKey,
            @Value("${app.level4.max-clock-drift-seconds:60}") long maxClockDriftSeconds) {
        this.level4Client = level4Client;
        this.tapEventRepository = tapEventRepository;
        this.batchSize = Math.max(1, batchSize);
        this.sentEventRetention = sentEventRetention.isNegative() ? Duration.ZERO : sentEventRetention;
        this.deviceCode = requireDefaultText(deviceCode, "gate-device-1");
        this.stationCode = requireDefaultText(stationCode, "station-1");
        this.qrVerificationKey = qrVerificationKey == null ? "" : qrVerificationKey.trim();
        this.maxClockDriftSeconds = Math.max(0, maxClockDriftSeconds);
    }

    public GateValidationService(
            Level4Client level4Client,
            TapEventRepository tapEventRepository,
            int batchSize,
            Duration sentEventRetention) {
        this(level4Client, tapEventRepository, batchSize, sentEventRetention,
                "gate-device-1", "station-1", "", 60);
    }

    @Transactional
    public Boolean validateTicket(ValidationRequest request) {
        QrPayload qrPayload = parseQrPayload(request);
        if (qrPayload == null || isExpired(qrPayload.expiresAtEpochSeconds())) {
            return false;
        }

        boolean validSignature = qrVerificationKey.isBlank()
                || hmacSha256Base64Url(qrVerificationKey, qrPayload.dataToSign()).equals(qrPayload.hmac());
        if (validSignature) {
            recordTapEvent(request);
        }
        return validSignature;
    }

    @Transactional
    public SubmitBatchResponse recordTapEvent(ValidationRequest request) {
        TapEvent tapEvent = toQueuedTapEvent(request);
        tapEventRepository.save(tapEvent);
        return new SubmitBatchResponse(QUEUED_MESSAGE);
    }

    @Transactional
    @Scheduled(fixedDelayString = "${app.level4.scan-record-flush-delay-ms:30000}")
    public synchronized void flushValidationBatch() {
        List<TapEvent> batch = nextBatch();
        if (batch.isEmpty()) {
            return;
        }

        try {
            SubmitBatchResponse response = level4Client.sendBatch(toBatchRequest(batch));
            requireAcceptedBatchResponse(response);
            markSent(batch);
        } catch (RuntimeException exception) {
            markFailed(batch, exception.getMessage());
        }
    }

    @Transactional
    @Scheduled(fixedDelayString = "${app.level4.sent-event-cleanup-delay-ms:3600000}")
    public void deleteExpiredSentEvents() {
        LocalDateTime expiresBefore = LocalDateTime.now().minus(sentEventRetention);
        tapEventRepository.deleteByDeliveryStatusAndSentAtBefore(STATUS_SENT, expiresBefore);
    }

    private List<TapEvent> nextBatch() {
        return tapEventRepository.findByDeliveryStatusInOrderByRecordedAtAsc(
                RETRYABLE_STATUSES, PageRequest.of(0, batchSize));
    }

    private void markSent(List<TapEvent> batch) {
        LocalDateTime sentAt = LocalDateTime.now();
        batch.forEach(event -> {
            event.setDeliveryStatus(STATUS_SENT);
            event.setSentAt(sentAt);
            event.setDeliveryError(null);
        });
        tapEventRepository.saveAll(batch);
    }

    private void requireAcceptedBatchResponse(SubmitBatchResponse response) {
        if (response == null || response.getMessage() == null || response.getMessage().isBlank()) {
            throw new IllegalStateException("Level 4 returned an incomplete scan batch response");
        }
    }

    private TapEvent toQueuedTapEvent(ValidationRequest request) {
        QrPayload qrPayload = parseQrPayload(request);
        if (qrPayload == null) {
            throw new IllegalArgumentException("QR payload is invalid");
        }

        TapEvent tapEvent = new TapEvent();
        // tapEvent.setEventId(UUID.randomUUID().toString());
        // tapEvent.setTicketId(qrPayload.qrId());
        // tapEvent.setGateId(deviceCode);
        // tapEvent.setStationId(stationCode);
        tapEvent.setQrPayload(request.getQrPayload());
        tapEvent.setEventType(request.getEventType());
        tapEvent.setRecordedAt(LocalDateTime.now());
        tapEvent.setDeliveryStatus(STATUS_PENDING);
        return tapEvent;
    }

    // private ValidationRequest normalize(ValidationRequest request) {
    //     if (request == null) {
    //         throw new IllegalArgumentException("Validation record is required");
    //     }

    //     ValidationRequest normalized = new ValidationRequest();
    //     normalized.setTicketId(requireText(request.getTicketId(), "Ticket ID"));
    //     normalized.setGateId(requireText(request.getGateId(), "Gate ID"));
    //     normalized.setStationId(requireText(request.getStationId(), "Station ID"));

    //     if (request.getEventType() == null) {
    //         throw new IllegalArgumentException("Event type is required");
    //     }
    //     normalized.setEventType(request.getEventType());
    //     return normalized;
    // }

    // private ExternalGateEventRequest toExternalRequest(TapEvent event) {
    //     return ExternalGateEventRequest.builder()
    //             .eventId(event.getEventId())
    //             .ticketId(event.getTicketId())
    //             .eventType(event.getEventType())
    //             .gateId(event.getGateId())
    //             .stationId(event.getStationId())
    //             .recordedAt(event.getRecordedAt())
    //             .source("GATE")
    //             .build();
    // }

    private SubmitBatchRequest toBatchRequest(List<TapEvent> events) {
        return SubmitBatchRequest.builder()
                .batchId(UUID.randomUUID().toString())
                .generatedAt(LocalDateTime.now())
                .records(events.stream()
                        .toList())
                .build();
    }

    private void markFailed(List<TapEvent> batch, String error) {
        String trimmedError = trimError(error);
        batch.forEach(event -> {
            event.setDeliveryStatus(STATUS_FAILED);
            event.setSentAt(null);
            event.setDeliveryError(trimmedError);
        });
        tapEventRepository.saveAll(batch);
    }

    private String trimError(String error) {
        if (error == null || error.isBlank()) {
            return "Unknown batch delivery error";
        }
        return error.length() <= MAX_ERROR_LENGTH ? error : error.substring(0, MAX_ERROR_LENGTH);
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String requireDefaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private QrPayload parseQrPayload(ValidationRequest request) {
        if (request == null || request.getQrPayload() == null || request.getQrPayload().isBlank()) {
            return null;
        }
        String[] parts = request.getQrPayload().trim().split(":");
        if (parts.length != 5 || !"AFCQR".equals(parts[0]) || !"v1".equals(parts[1])
                || !parts[3].startsWith("exp=") || !parts[4].startsWith("hmac=")) {
            return null;
        }
        try {
            String qrId = requireText(parts[2], "QR ID");
            long expiresAt = Long.parseLong(parts[3].substring("exp=".length()));
            String hmac = requireText(parts[4].substring("hmac=".length()), "QR HMAC");
            return new QrPayload(qrId, expiresAt, hmac);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean isExpired(long expiresAtEpochSeconds) {
        return System.currentTimeMillis() / 1000 > expiresAtEpochSeconds + maxClockDriftSeconds;
    }

    private String hmacSha256Base64Url(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("Could not verify QR signature", exception);
        }
    }

    private record QrPayload(String qrId, long expiresAtEpochSeconds, String hmac) {
        private String dataToSign() {
            return "AFCQR:v1:" + qrId + ":exp=" + expiresAtEpochSeconds;
        }
    }
}
