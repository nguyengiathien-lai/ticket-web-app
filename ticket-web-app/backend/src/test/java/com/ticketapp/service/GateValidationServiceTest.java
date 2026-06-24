package com.ticketapp.service;

import com.ticketapp.client.level4.Level4Client;
import com.ticketapp.dto.external.ExternalGateEventBatchRequest;
import com.ticketapp.dto.external.ExternalGateEventRequest;
import com.ticketapp.dto.gate.GateEventType;
import com.ticketapp.dto.gate.ValidationRecordRequest;
import com.ticketapp.dto.gate.ValidationRecordResponse;
import com.ticketapp.entity.GateEvent;
import com.ticketapp.repository.GateEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;

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

    @Test
    void storesTheScanRecordForBatchDelivery() {
        Level4Client client = mock(Level4Client.class);
        GateEventRepository repository = mock(GateEventRepository.class);
        GateValidationService service = new GateValidationService(client, repository, 100);
        ValidationRecordRequest request = new ValidationRecordRequest();
        request.setTicketId(" ticket-42 ");
        request.setGateId(" gate-1 ");
        request.setStationId(" station-1 ");
        request.setEventType(GateEventType.CHECK_IN);
        request.setRecordedTime(LocalDateTime.of(2026, 6, 21, 15, 30));

        ValidationRecordResponse response = service.recordValidation(request);

        ArgumentCaptor<GateEvent> captor = ArgumentCaptor.forClass(GateEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTicketId()).isEqualTo("ticket-42");
        assertThat(captor.getValue().getGateId()).isEqualTo("gate-1");
        assertThat(captor.getValue().getStationId()).isEqualTo("station-1");
        assertThat(captor.getValue().getEventType()).isEqualTo(GateEventType.CHECK_IN);
        assertThat(captor.getValue().getRecordedAt())
                .isEqualTo(LocalDateTime.of(2026, 6, 21, 15, 30));
        assertThat(captor.getValue().getDeliveryStatus()).isEqualTo("PENDING");
        assertThat(captor.getValue().getEventId()).isNotBlank();
        assertThat(response.getMessage()).isEqualTo("Scan record queued for batch delivery");
        assertThat(request.getTicketId()).isEqualTo(" ticket-42 ");
        verify(client, never()).sendBatch(any(ExternalGateEventBatchRequest.class));
    }

    @Test
    void rejectsMissingRequiredDataWhenCalledOutsideTheController() {
        GateValidationService service = new GateValidationService(
                mock(Level4Client.class), mock(GateEventRepository.class), 100);

        assertThatThrownBy(() -> service.recordValidation(new ValidationRecordRequest()))
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
        GateValidationService service = new GateValidationService(client, repository, 50);

        service.flushValidationBatch();

        ArgumentCaptor<ExternalGateEventBatchRequest> batchCaptor =
                ArgumentCaptor.forClass(ExternalGateEventBatchRequest.class);
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
        GateValidationService service = new GateValidationService(client, repository, 50);

        service.flushValidationBatch();

        assertThat(event.getDeliveryStatus()).isEqualTo("FAILED");
        assertThat(event.getSentAt()).isNull();
        assertThat(event.getDeliveryError()).isEqualTo("Level 4 unavailable");
        verify(repository).saveAll(List.of(event));
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
