package com.validationgate.service;

import com.validationgate.client.Level4Client;
import com.validationgate.dto.ExternalGateEventBatchRequest;
import com.validationgate.dto.ExternalGateEventRequest;
import com.validationgate.dto.ValidationRecordRequest;
import com.validationgate.dto.ValidationRecordResponse;
import com.validationgate.entity.GateEvent;
import com.validationgate.repository.GateEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final GateEventRepository gateEventRepository;
    private final int batchSize;

    public GateValidationService(
            Level4Client level4Client,
            GateEventRepository gateEventRepository,
            @Value("${app.level4.scan-record-batch-size:100}") int batchSize) {
        this.level4Client = level4Client;
        this.gateEventRepository = gateEventRepository;
        this.batchSize = Math.max(1, batchSize);
    }

    @Transactional
    public ValidationRecordResponse recordValidation(ValidationRecordRequest request) {
        GateEvent gateEvent = toQueuedGateEvent(request);
        gateEventRepository.save(gateEvent);
        return new ValidationRecordResponse(QUEUED_MESSAGE);
    }

    @Transactional
    @Scheduled(fixedDelayString = "${app.level4.scan-record-flush-delay-ms:30000}")
    public synchronized void flushValidationBatch() {
        List<GateEvent> batch = nextBatch();
        if (batch.isEmpty()) {
            return;
        }

        try {
            ValidationRecordResponse response = level4Client.sendBatch(toExternalBatchRequest(batch));
            requireAcceptedBatchResponse(response);
            markSent(batch);
        } catch (RuntimeException exception) {
            markFailed(batch, exception.getMessage());
        }
    }

    private List<GateEvent> nextBatch() {
        return gateEventRepository.findByDeliveryStatusInOrderByRecordedAtAsc(
                RETRYABLE_STATUSES, PageRequest.of(0, batchSize));
    }

    private void markSent(List<GateEvent> batch) {
        LocalDateTime sentAt = LocalDateTime.now();
        batch.forEach(event -> {
            event.setDeliveryStatus(STATUS_SENT);
            event.setSentAt(sentAt);
            event.setDeliveryError(null);
        });
        gateEventRepository.saveAll(batch);
    }

    private void requireAcceptedBatchResponse(ValidationRecordResponse response) {
        if (response == null || response.getMessage() == null || response.getMessage().isBlank()) {
            throw new IllegalStateException("Level 4 returned an incomplete scan batch response");
        }
    }

    private GateEvent toQueuedGateEvent(ValidationRecordRequest request) {
        ValidationRecordRequest normalized = normalize(request);

        GateEvent gateEvent = new GateEvent();
        gateEvent.setEventId(UUID.randomUUID().toString());
        gateEvent.setTicketId(normalized.getTicketId());
        gateEvent.setGateId(normalized.getGateId());
        gateEvent.setStationId(normalized.getStationId());
        gateEvent.setEventType(normalized.getEventType());
        gateEvent.setRecordedAt(normalized.getRecordedTime());
        gateEvent.setDeliveryStatus(STATUS_PENDING);
        return gateEvent;
    }

    private ValidationRecordRequest normalize(ValidationRecordRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Validation record is required");
        }

        ValidationRecordRequest normalized = new ValidationRecordRequest();
        normalized.setTicketId(requireText(request.getTicketId(), "Ticket ID"));
        normalized.setGateId(requireText(request.getGateId(), "Gate ID"));
        normalized.setStationId(requireText(request.getStationId(), "Station ID"));

        if (request.getEventType() == null) {
            throw new IllegalArgumentException("Event type is required");
        }
        if (request.getRecordedTime() == null) {
            throw new IllegalArgumentException("Recorded time is required");
        }
        normalized.setEventType(request.getEventType());
        normalized.setRecordedTime(request.getRecordedTime());
        return normalized;
    }

    private ExternalGateEventRequest toExternalRequest(GateEvent event) {
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

    private ExternalGateEventBatchRequest toExternalBatchRequest(List<GateEvent> events) {
        return ExternalGateEventBatchRequest.builder()
                .batchId(UUID.randomUUID().toString())
                .generatedAt(LocalDateTime.now())
                .records(events.stream()
                        .map(this::toExternalRequest)
                        .toList())
                .build();
    }

    private void markFailed(List<GateEvent> batch, String error) {
        String trimmedError = trimError(error);
        batch.forEach(event -> {
            event.setDeliveryStatus(STATUS_FAILED);
            event.setSentAt(null);
            event.setDeliveryError(trimmedError);
        });
        gateEventRepository.saveAll(batch);
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
