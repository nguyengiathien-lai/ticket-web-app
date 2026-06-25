package com.validationgate.service;

import com.validationgate.client.Level4Client;
import com.validationgate.dto.ExternalGateEventRequest;
import com.validationgate.dto.RecordRequestBatch;
import com.validationgate.dto.ValidationRequest;
import com.validationgate.dto.ValidationRecordResponse;
import com.validationgate.entity.TapEvent;
import com.validationgate.repository.TapEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

    public GateValidationService(
            Level4Client level4Client,
            TapEventRepository tapEventRepository,
            @Value("${app.level4.scan-record-batch-size:100}") int batchSize,
            @Value("${app.level4.sent-event-retention:2d}") Duration sentEventRetention) {
        this.level4Client = level4Client;
        this.tapEventRepository = tapEventRepository;
        this.batchSize = Math.max(1, batchSize);
        this.sentEventRetention = sentEventRetention.isNegative() ? Duration.ZERO : sentEventRetention;
    }

    @Transactional
    public ValidationRecordResponse ticketValidation(ValidationRequest request) {
        return submitTapEvent(request);
    }

    @Transactional
    public ValidationRecordResponse submitTapEvent(ValidationRequest request) {
        TapEvent tapEvent = toQueuedTapEvent(request);
        tapEventRepository.save(tapEvent);
        return new ValidationRecordResponse(QUEUED_MESSAGE);
    }

    @Transactional
    @Scheduled(fixedDelayString = "${app.level4.scan-record-flush-delay-ms:30000}")
    public synchronized void flushValidationBatch() {
        List<TapEvent> batch = nextBatch();
        if (batch.isEmpty()) {
            return;
        }

        try {
            ValidationRecordResponse response = level4Client.sendBatch(toBatchRequest(batch));
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

    private void requireAcceptedBatchResponse(ValidationRecordResponse response) {
        if (response == null || response.getMessage() == null || response.getMessage().isBlank()) {
            throw new IllegalStateException("Level 4 returned an incomplete scan batch response");
        }
    }

    private TapEvent toQueuedTapEvent(ValidationRequest request) {
        ValidationRequest normalized = normalize(request);

        TapEvent tapEvent = new TapEvent();
        tapEvent.setEventId(UUID.randomUUID().toString());
        tapEvent.setTicketId(normalized.getTicketId());
        tapEvent.setGateId(normalized.getGateId());
        tapEvent.setStationId(normalized.getStationId());
        tapEvent.setEventType(normalized.getEventType());
        tapEvent.setRecordedAt(LocalDateTime.now());
        tapEvent.setDeliveryStatus(STATUS_PENDING);
        return tapEvent;
    }

    private ValidationRequest normalize(ValidationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Validation record is required");
        }

        ValidationRequest normalized = new ValidationRequest();
        normalized.setTicketId(requireText(request.getTicketId(), "Ticket ID"));
        normalized.setGateId(requireText(request.getGateId(), "Gate ID"));
        normalized.setStationId(requireText(request.getStationId(), "Station ID"));

        if (request.getEventType() == null) {
            throw new IllegalArgumentException("Event type is required");
        }
        normalized.setEventType(request.getEventType());
        return normalized;
    }

    private ExternalGateEventRequest toExternalRequest(TapEvent event) {
        return ExternalGateEventRequest.builder()
                .eventId(event.getEventId())
                .ticketId(event.getTicketId())
                .eventType(event.getEventType())
                .gateId(event.getGateId())
                .stationId(event.getStationId())
                .recordedAt(event.getRecordedAt())
                .source("GATE")
                .build();
    }

    private RecordRequestBatch toBatchRequest(List<TapEvent> events) {
        return RecordRequestBatch.builder()
                .batchId(UUID.randomUUID().toString())
                .generatedAt(LocalDateTime.now())
                .records(events.stream()
                        .map(this::toExternalRequest)
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
}
