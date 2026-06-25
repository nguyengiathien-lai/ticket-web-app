package com.validationgate.service;

import com.validationgate.client.Level4Client;
import com.validationgate.dto.ExternalGateEventRequest;
import com.validationgate.dto.GateEventType;
import com.validationgate.dto.RecordRequestBatch;
import com.validationgate.dto.ValidationRequest;
import com.validationgate.dto.ValidationRecordResponse;
import com.validationgate.entity.GateEvent;
import com.validationgate.repository.GateEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GateValidationServiceTest {

    private static final Duration SENT_EVENT_RETENTION = Duration.ofDays(7);

    @Test
    void storesTheScanRecordForBatchDelivery() {
        Level4Client client = mock(Level4Client.class);
        GateEventRepository repository = mock(GateEventRepository.class);
        GateValidationService service = new GateValidationService(client, repository, 100, SENT_EVENT_RETENTION);
        ValidationRequest request = new ValidationRequest();
        request.setTicketId(" ticket-42 ");
        request.setGateId(" gate-1 ");
        request.setStationId(" station-1 ");
        request.setEventType(GateEventType.CHECK_IN);
        LocalDateTime beforeRecord = LocalDateTime.now();

        ValidationRecordResponse response = service.recordValidation(request);
        LocalDateTime afterRecord = LocalDateTime.now();

        ArgumentCaptor<GateEvent> captor = ArgumentCaptor.forClass(GateEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTicketId()).isEqualTo("ticket-42");
        assertThat(captor.getValue().getGateId()).isEqualTo("gate-1");
        assertThat(captor.getValue().getStationId()).isEqualTo("station-1");
        assertThat(captor.getValue().getEventType()).isEqualTo(GateEventType.CHECK_IN);
        assertThat(captor.getValue().getRecordedAt())
                .isBetween(beforeRecord, afterRecord);
        assertThat(captor.getValue().getDeliveryStatus()).isEqualTo("PENDING");
        assertThat(captor.getValue().getEventId()).isNotBlank();
        assertThat(response.getMessage()).isEqualTo("Scan record queued for batch delivery");
        assertThat(request.getTicketId()).isEqualTo(" ticket-42 ");
        verify(client, never()).sendBatch(any(RecordRequestBatch.class));
    }

    @Test
    void rejectsMissingRequiredDataWhenCalledOutsideTheController() {
        GateValidationService service = new GateValidationService(
                mock(Level4Client.class), mock(GateEventRepository.class), 100, SENT_EVENT_RETENTION);

        assertThatThrownBy(() -> service.recordValidation(new ValidationRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ticket ID is required");
    }

    @Test
    void sendsPendingRecordsAsABatchAndMarksThemSent() {
        Level4Client client = mock(Level4Client.class);
        GateEventRepository repository = mock(GateEventRepository.class);
        GateEvent event = event("event-1");
        when(repository.findByDeliveryStatusInOrderByRecordedAtAsc(
                anyCollection(), any(Pageable.class))).thenReturn(List.of(event));
        when(client.sendBatch(any())).thenReturn(new ValidationRecordResponse("Batch received"));
        GateValidationService service = new GateValidationService(client, repository, 50, SENT_EVENT_RETENTION);

        service.flushValidationBatch();

        ArgumentCaptor<RecordRequestBatch> batchCaptor =
                ArgumentCaptor.forClass(RecordRequestBatch.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByDeliveryStatusInOrderByRecordedAtAsc(anyCollection(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);

        verify(client).sendBatch(batchCaptor.capture());
        assertThat(batchCaptor.getValue().getBatchId()).isNotBlank();
        assertThat(batchCaptor.getValue().getGeneratedAt()).isNotNull();
        assertThat(batchCaptor.getValue().getRecords()).hasSize(1);
        ExternalGateEventRequest outboundRecord = batchCaptor.getValue().getRecords().get(0);
        assertThat(outboundRecord.getEventId()).isEqualTo("event-1");
        assertThat(outboundRecord.getSource()).isEqualTo("GATE");
        assertThat(event.getDeliveryStatus()).isEqualTo("SENT");
        assertThat(event.getSentAt()).isNotNull();
        assertThat(event.getDeliveryError()).isNull();
        verify(repository).saveAll(List.of(event));
    }

    @Test
    void marksTheBatchFailedWhenLevel4RejectsIt() {
        Level4Client client = mock(Level4Client.class);
        GateEventRepository repository = mock(GateEventRepository.class);
        GateEvent event = event("event-1");
        when(repository.findByDeliveryStatusInOrderByRecordedAtAsc(
                anyCollection(), any(Pageable.class))).thenReturn(List.of(event));
        when(client.sendBatch(any())).thenThrow(new IllegalStateException("Level 4 unavailable"));
        GateValidationService service = new GateValidationService(client, repository, 50, SENT_EVENT_RETENTION);

        service.flushValidationBatch();

        assertThat(event.getDeliveryStatus()).isEqualTo("FAILED");
        assertThat(event.getSentAt()).isNull();
        assertThat(event.getDeliveryError()).isEqualTo("Level 4 unavailable");
        verify(repository).saveAll(List.of(event));
    }

    @Test
    void deletesSentRecordsAfterTheRetentionWindow() {
        GateEventRepository repository = mock(GateEventRepository.class);
        GateValidationService service = new GateValidationService(
                mock(Level4Client.class), repository, 100, Duration.ofHours(2));
        LocalDateTime beforeCleanup = LocalDateTime.now().minusHours(2);

        service.deleteExpiredSentEvents();

        ArgumentCaptor<LocalDateTime> thresholdCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).deleteByDeliveryStatusAndSentAtBefore(
                org.mockito.ArgumentMatchers.eq("SENT"), thresholdCaptor.capture());
        assertThat(thresholdCaptor.getValue()).isAfterOrEqualTo(beforeCleanup);
        assertThat(thresholdCaptor.getValue()).isBeforeOrEqualTo(LocalDateTime.now().minusHours(2));
    }

    private GateEvent event(String eventId) {
        GateEvent event = new GateEvent();
        event.setEventId(eventId);
        event.setTicketId("ticket-42");
        event.setGateId("gate-1");
        event.setStationId("station-1");
        event.setEventType(GateEventType.CHECK_OUT);
        event.setRecordedAt(LocalDateTime.of(2026, 6, 21, 15, 30));
        event.setDeliveryStatus("PENDING");
        return event;
    }
}
