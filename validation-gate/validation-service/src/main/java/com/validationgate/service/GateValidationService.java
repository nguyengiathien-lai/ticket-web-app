package com.validationgate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.validationgate.client.Level4Client;
import com.validationgate.dto.SubmitBatchRequest;
import com.validationgate.dto.ValidationRequest;
import com.validationgate.dto.SubmitBatchResponse;
import com.validationgate.entity.DeviceConfigPackage;
import com.validationgate.entity.TapEvent;
import com.validationgate.repository.DeviceConfigPackageRepository;
import com.validationgate.repository.TapEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
public class GateValidationService {

    private static final Logger log = LoggerFactory.getLogger(GateValidationService.class);

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_FAILED = "FAILED";
    private static final List<String> RETRYABLE_STATUSES = List.of(STATUS_PENDING, STATUS_FAILED);
    private static final String QUEUED_MESSAGE = "Scan record queued for batch delivery";
    private static final String EMPTY_BATCH_MESSAGE = "No scan records pending for batch delivery";
    private static final String SENT_BATCH_MESSAGE = "Scan record batch delivered successfully";
    private static final String FAILED_BATCH_MESSAGE = "Scan record batch delivery failed and records were marked for retry";
    private static final int MAX_ERROR_LENGTH = 500;
    private static final String QR_ALGORITHM_HMAC_SHA256 = "HMAC_SHA256";

    private final Level4Client level4Client;
    private final TapEventRepository tapEventRepository;
    private final DeviceConfigPackageRepository deviceConfigRepository;
    private final ObjectMapper objectMapper;
    private final int batchSize;
    private final Duration sentEventRetention;

    public GateValidationService(
            Level4Client level4Client,
            TapEventRepository tapEventRepository,
            DeviceConfigPackageRepository deviceConfigRepository,
            ObjectMapper objectMapper,
            @Value("${app.level4.scan-record-batch-size:2}") int batchSize,
            @Value("${app.level4.sent-event-retention:2d}") Duration sentEventRetention) {
        this.level4Client = level4Client;
        this.tapEventRepository = tapEventRepository;
        this.deviceConfigRepository = deviceConfigRepository;
        this.objectMapper = objectMapper;
        this.batchSize = Math.max(1, batchSize);
        this.sentEventRetention = sentEventRetention.isNegative() ? Duration.ZERO : sentEventRetention;
    }

    @Transactional
    public Boolean validateTicket(ValidationRequest request) {
        String secretHmac = "my-secret-key-for-qr-hmac-signing";
        String qrVerificationKey = Base64.getUrlEncoder()
                                        .withoutPadding()
                                        .encodeToString(secretHmac.getBytes(StandardCharsets.UTF_8));     
        
        log.info("QR verification key generated for testing; qrVerificationKeyPreview={}", qrVerificationKey);
        log.info("Starting ticket validation request; deviceCode={}, stationCode={}, eventType={}, payloadPresent={}, payloadLength={}",
                request == null ? null : request.getDeviceCode(),
                request == null ? null : request.getStationCode(),
                request == null ? null : request.getEventType(),
                request != null && request.getQrPayload() != null && !request.getQrPayload().isBlank(),
                request == null || request.getQrPayload() == null ? 0 : request.getQrPayload().length());

        ScanContext scanContext = scanContext(request);
        log.debug("Validation scan context resolved; deviceCode={}, stationCode={}",
                scanContext.deviceCode(), scanContext.stationCode());

        QrPayload qrPayload = parseQrPayload(request);
        if (qrPayload == null) {
            log.warn("Ticket validation rejected: QR payload is missing or malformed; deviceCode={}, stationCode={}, eventType={}, payloadPreview={}",
                    scanContext.deviceCode(), scanContext.stationCode(), request.getEventType(),
                    preview(request.getQrPayload()));
            return false;
        }
        log.info("QR payload extracted; ticketId={}, expiresAtEpochSeconds={}, expiresAt={}, hmacPreview={}, dataToSign={}",
                qrPayload.ticketId(),
                qrPayload.expiresAtEpochSeconds(),
                Instant.ofEpochSecond(qrPayload.expiresAtEpochSeconds()),
                mask(qrPayload.hmac()),
                qrPayload.dataToSign());

        DeviceConfig deviceConfig = loadDeviceConfig(scanContext);
        if (deviceConfig == null) {
            log.warn("Ticket validation rejected: no device config found for station/device; stationCode={}, deviceCode={}",
                    scanContext.stationCode(), scanContext.deviceCode());
            return false;
        }
        log.info("Device config loaded; stationCode={}, deviceCode={}, algorithm={}, allowOfflineValidation={}, maxClockDriftSeconds={}, verificationKeyPresent={}, verificationKeyPreview={}",
                scanContext.stationCode(),
                scanContext.deviceCode(),
                deviceConfig.qrVerificationAlgorithm(),
                deviceConfig.allowOfflineValidation(),
                deviceConfig.maxClockDriftSeconds(),
                deviceConfig.qrVerificationKey() != null && !deviceConfig.qrVerificationKey().isBlank(),
                mask(deviceConfig.qrVerificationKey()));

        if (!deviceConfig.allowsQrValidation()) {
            log.warn("Ticket validation rejected: device config does not allow QR validation; algorithm={}, allowOfflineValidation={}, verificationKeyPresent={}",
                    deviceConfig.qrVerificationAlgorithm(),
                    deviceConfig.allowOfflineValidation(),
                    deviceConfig.qrVerificationKey() != null && !deviceConfig.qrVerificationKey().isBlank());
            return false;
        }

        long currentEpochSeconds = System.currentTimeMillis() / 1000;
        boolean expired = isExpired(qrPayload.expiresAtEpochSeconds(), deviceConfig.maxClockDriftSeconds());
        log.info("QR expiry check; ticketId={}, nowEpochSeconds={}, expiresAtEpochSeconds={}, maxClockDriftSeconds={}, acceptedUntilEpochSeconds={}, expired={}",
                qrPayload.ticketId(),
                currentEpochSeconds,
                qrPayload.expiresAtEpochSeconds(),
                deviceConfig.maxClockDriftSeconds(),
                qrPayload.expiresAtEpochSeconds() + deviceConfig.maxClockDriftSeconds(),
                expired);
        if (expired) {
            log.warn("Ticket validation rejected: QR payload expired; ticketId={}, now={}, expiresAt={}, maxClockDriftSeconds={}",
                    qrPayload.ticketId(),
                    Instant.ofEpochSecond(currentEpochSeconds),
                    Instant.ofEpochSecond(qrPayload.expiresAtEpochSeconds()),
                    deviceConfig.maxClockDriftSeconds());
            return false;
        }

        SignatureCheck signatureCheck = checkSignature(deviceConfig, qrPayload);
        log.info("QR signature check; ticketId={}, expectedHmacPreview={}, receivedHmacPreview={}, valid={}",
                qrPayload.ticketId(),
                mask(signatureCheck.expectedHmac()),
                mask(qrPayload.hmac()),
                signatureCheck.valid());
        if (!signatureCheck.valid()) {
            log.warn("Ticket validation rejected: QR signature mismatch; ticketId={}, dataToSign={}, expectedHmacPreview={}, receivedHmacPreview={}",
                    qrPayload.ticketId(),
                    qrPayload.dataToSign(),
                    mask(signatureCheck.expectedHmac()),
                    mask(qrPayload.hmac()));
            return false;
        }

        recordTapEvent(request);
        log.info("Ticket validation accepted; ticketId={}, deviceCode={}, stationCode={}, eventType={}",
                qrPayload.ticketId(), scanContext.deviceCode(), scanContext.stationCode(), request.getEventType());
        return true;
    }

    @Transactional
    public void recordTapEvent(ValidationRequest request) {
        TapEvent tapEvent = toQueuedTapEvent(request);
        TapEvent savedEvent = tapEventRepository.save(tapEvent);
        TapEvent loggedEvent = savedEvent == null ? tapEvent : savedEvent;
        log.info("Tap event queued; tapEventId={}, eventType={}, recordedAt={}, deliveryStatus={}, payloadPreview={}",
                loggedEvent.getId(),
                loggedEvent.getEventType(),
                loggedEvent.getRecordedAt(),
                loggedEvent.getDeliveryStatus(),
                preview(loggedEvent.getQrPayload()));
        // return new SubmitBatchResponse(QUEUED_MESSAGE);
    }

    @Transactional
    // @Scheduled(fixedDelayString = "${app.level4.scan-record-flush-delay-ms:30000}")
    // public synchronized void flushValidationBatch() {
    public SubmitBatchResponse flushValidationBatch() {
        List<TapEvent> batch = nextBatch();
        if (batch.isEmpty()) {
            return response(0, 0, 0, EMPTY_BATCH_MESSAGE);
        }

        try {
            SubmitBatchResponse response = level4Client.sendBatch(toBatchRequest(batch));
            requireAcceptedBatchResponse(response);
            markSent(batch);
            return response;
        } catch (RuntimeException exception) {
            String error = trimError(exception.getMessage());
            log.error("Scan record batch delivery failed; records will be retried. batchSize={}, error={}",
                    batch.size(), error, exception);
            markFailed(batch, error);
            return response(batch.size(), 0, batch.size(), FAILED_BATCH_MESSAGE + ": " + error);
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
        if (response == null) {
            throw new IllegalStateException("Level 4 returned an incomplete scan batch response");
        }
        if (response.getFailed() > 0 || response.getErrors() != null && !response.getErrors().isEmpty()) {
            throw new IllegalStateException("Level 4 rejected one or more scan records");
        }
    }

    private TapEvent toQueuedTapEvent(ValidationRequest request) {
        QrPayload qrPayload = parseQrPayload(request);
        if (qrPayload == null) {
            throw new IllegalArgumentException("QR payload is invalid");
        }

        TapEvent tapEvent = new TapEvent();
        // tapEvent.setEventId(UUID.randomUUID().toString());
        // tapEvent.setTicketId(qrPayload.ticketId());
        // tapEvent.setGateId(scanContext(request).deviceCode());
        // tapEvent.setStationId(scanContext(request).stationCode());
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

    private SubmitBatchRequest.BatchTransactionItem toBatchItem(TapEvent event) {
        return SubmitBatchRequest.BatchTransactionItem.builder()
                .qrPayload(event.getQrPayload())
                .tapType(event.getEventType() == null ? null : event.getEventType().name())
                .occurredAt(event.getRecordedAt())
                .build();
    }

    private SubmitBatchRequest toBatchRequest(List<TapEvent> events) {
        return SubmitBatchRequest.builder()
                .transactions(events.stream()
                        .map(this::toBatchItem) 
                        .toList())
                .build();
    }

    private SubmitBatchResponse response(int total, int success, int failed, String error) {
        List<String> errors = new ArrayList<>();
        if (error != null && !error.isBlank()) {
            errors.add(error);
        }
        return SubmitBatchResponse.builder()
                .total(total)
                .success(success)
                .failed(failed)
                .errors(errors)
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
            String ticketId = requireText(parts[2], "Ticket ID");
            long expiresAt = Long.parseLong(parts[3].substring("exp=".length()));
            String hmac = requireText(parts[4].substring("hmac=".length()), "QR HMAC");
            return new QrPayload(ticketId, expiresAt, hmac);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private ScanContext scanContext(ValidationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Validation request is required");
        }
        return new ScanContext(
                requireText(request.getDeviceCode(), "Device code"),
                requireText(request.getStationCode(), "Station code"));
    }

    private DeviceConfig loadDeviceConfig(ScanContext scanContext) {
        log.debug("Loading device config package; stationCode={}, deviceCode={}",
                scanContext.stationCode(), scanContext.deviceCode());
        return deviceConfigRepository.findByStationCode(scanContext.stationCode())
                .map(packageEntity -> {
                    log.debug("Device config package found by station; stationCode={}, packageDeviceCode={}, requestedDeviceCode={}, packageId={}",
                            scanContext.stationCode(),
                            packageEntity.getDeviceCode(),
                            scanContext.deviceCode(),
                            packageEntity.getPackageId());
                    return packageEntity;
                })
                .filter(packageEntity -> scanContext.deviceCode().equals(packageEntity.getDeviceCode()))
                .map(this::toDeviceConfig)
                .orElse(null);
    }

    private DeviceConfig toDeviceConfig(DeviceConfigPackage packageEntity) {
        if (packageEntity.getQrVerificationAlgorithm() != null || packageEntity.getQrVerificationKey() != null
                || packageEntity.getMaxClockDriftSeconds() != null || packageEntity.getAllowOfflineValidation() != null) {
            log.debug("Using flattened device config columns; packageId={}, stationCode={}, configVersion={}",
                    packageEntity.getPackageId(), packageEntity.getStationCode(), packageEntity.getVersion());
            return new DeviceConfig(
                    packageEntity.getQrVerificationAlgorithm(),
                    packageEntity.getQrVerificationKey(),
                    Math.max(0, packageEntity.getMaxClockDriftSeconds() == null
                            ? 0
                            : packageEntity.getMaxClockDriftSeconds()),
                    Boolean.TRUE.equals(packageEntity.getAllowOfflineValidation()));
        }
        log.debug("Using device config JSON payload; packageId={}, stationCode={}, configVersion={}",
                packageEntity.getPackageId(), packageEntity.getStationCode(), packageEntity.getVersion());
        return DeviceConfig.fromJson(readJson(packageEntity.getPayloadJson()));
    }

    private SignatureCheck checkSignature(DeviceConfig deviceConfig, QrPayload qrPayload) {
        String expectedHmac = hmacSha256Base64Url(decodeQrVerificationKey(deviceConfig.qrVerificationKey()), qrPayload.dataToSign());
        return new SignatureCheck(expectedHmac, expectedHmac.equals(qrPayload.hmac()));
    }

    private JsonNode readJson(String payloadJson) {
        try {
            return objectMapper.readTree(payloadJson);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored device package JSON is invalid", exception);
        }
    }

    private boolean isExpired(long expiresAtEpochSeconds, long maxClockDriftSeconds) {
        return System.currentTimeMillis() / 1000 > expiresAtEpochSeconds + maxClockDriftSeconds;
    }

    private String decodeQrVerificationKey(String qrVerificationKey) {
        byte[] decoded = Base64.getUrlDecoder().decode(qrVerificationKey);
    return new String(decoded, StandardCharsets.UTF_8);
}

    private String hmacSha256Base64Url(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretBytes(secret), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("Could not verify QR signature", exception);
        }
    }

    private byte[] secretBytes(String secret) {
        try {
            return Base64.getUrlDecoder().decode(secret);
        } catch (IllegalArgumentException exception) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }

    private String preview(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= 80 ? trimmed : trimmed.substring(0, 80) + "...";
    }

    private String mask(String value) {
        if (value == null || value.isBlank()) {
            return "<blank>";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 8) {
            return "***" + trimmed.length();
        }
        return trimmed.substring(0, 4) + "..." + trimmed.substring(trimmed.length() - 4);
    }

    private record ScanContext(String deviceCode, String stationCode) {
    }

    private record QrPayload(String ticketId, long expiresAtEpochSeconds, String hmac) {
        private String dataToSign() {
            return "AFCQR:v1:" + ticketId + ":exp=" + expiresAtEpochSeconds;
        }
    }

    private record SignatureCheck(String expectedHmac, boolean valid) {
    }

    private record DeviceConfig(
            String qrVerificationAlgorithm,
            String qrVerificationKey,
            long maxClockDriftSeconds,
            boolean allowOfflineValidation) {

        private static DeviceConfig fromJson(JsonNode payload) {
            return new DeviceConfig(
                    payload.path("qrVerificationAlgorithm").asText(),
                    payload.path("qrVerificationKey").asText(),
                    Math.max(0, payload.path("maxClockDriftSeconds").asLong(0)),
                    payload.path("allowOfflineValidation").asBoolean(true));
        }

        private boolean allowsQrValidation() {
            return allowOfflineValidation
                    && QR_ALGORITHM_HMAC_SHA256.equals(qrVerificationAlgorithm)
                    && qrVerificationKey != null
                    && !qrVerificationKey.isBlank();
        }
    }
}
