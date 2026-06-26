package com.validationgate.service;

import com.validationgate.client.Level4Client;
import com.validationgate.dto.SubmitBatchRequest;
import com.validationgate.dto.SubmitBatchResponse;
import com.validationgate.dto.TapEventType;
import com.validationgate.dto.ValidationRequest;
import com.validationgate.entity.TapEvent;
import com.validationgate.repository.TapEventRepository;
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
    void validatesQrPayloadAndStoresTheScanRecordForBatchDelivery() {
        Level4Client client = mock(Level4Client.class);
        TapEventRepository repository = mock(TapEventRepository.class);
        GateValidationService service = new GateValidationService(
                client, repository, 100, SENT_EVENT_RETENTION, "device-1", "station-1", "", 60);
        ValidationRequest request = request("ticket-42");
        LocalDateTime beforeRecord = LocalDateTime.now();

        Boolean valid = service.validateTicket(request);
        LocalDateTime afterRecord = LocalDateTime.now();

        ArgumentCaptor<TapEvent> captor = ArgumentCaptor.forClass(TapEvent.class);
        verify(repository).save(captor.capture());
        assertThat(valid).isTrue();
        assertThat(captor.getValue().getTicketId()).isEqualTo("ticket-42");
        assertThat(captor.getValue().getGateId()).isEqualTo("device-1");
        assertThat(captor.getValue().getStationId()).isEqualTo("station-1");
        assertThat(captor.getValue().getEventType()).isEqualTo(TapEventType.CHECK_IN);
        assertThat(captor.getValue().getRecordedAt()).isBetween(beforeRecord, afterRecord);
        assertThat(captor.getValue().getDeliveryStatus()).isEqualTo("PENDING");
        assertThat(captor.getValue().getEventId()).isNotBlank();
        verify(client, never()).sendBatch(any(SubmitBatchRequest.class));
    }

    @Test
    void rejectsInvalidQrPayloadWhenCalledOutsideTheController() {
        GateValidationService service = new GateValidationService(
                mock(Level4Client.class), mock(TapEventRepository.class), 100, SENT_EVENT_RETENTION);
        ValidationRequest request = new ValidationRequest();
        request.setQrPayload("invalid");
        request.setEventType(TapEventType.CHECK_IN);

        assertThatThrownBy(() -> service.recordTapEvent(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("QR payload is invalid");
    }

    @Test
    void expiredQrPayloadIsDeniedAndNotStored() {
        TapEventRepository repository = mock(TapEventRepository.class);
        GateValidationService service = new GateValidationService(
                mock(Level4Client.class), repository, 100, SENT_EVENT_RETENTION,
                "device-1", "station-1", "", 0);
        ValidationRequest request = new ValidationRequest();
        request.setQrPayload("AFCQR:v1:ticket-42:exp=1:hmac=placeholder");
        request.setEventType(TapEventType.CHECK_IN);

        Boolean valid = service.validateTicket(request);

        assertThat(valid).isFalse();
        verify(repository, never()).save(any(TapEvent.class));
    }

    @Test
    void sendsPendingRecordsAsABatchAndMarksThemSent() {
        Level4Client client = mock(Level4Client.class);
        TapEventRepository repository = mock(TapEventRepository.class);
        TapEvent event = event("event-1");
        when(repository.findByDeliveryStatusInOrderByRecordedAtAsc(
                anyCollection(), any(Pageable.class))).thenReturn(List.of(event));
        when(client.sendBatch(any())).thenReturn(new SubmitBatchResponse("Batch received"));
        GateValidationService service = new GateValidationService(client, repository, 50, SENT_EVENT_RETENTION);

        service.flushValidationBatch();

        ArgumentCaptor<SubmitBatchRequest> batchCaptor = ArgumentCaptor.forClass(SubmitBatchRequest.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByDeliveryStatusInOrderByRecordedAtAsc(anyCollection(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);

        verify(client).sendBatch(batchCaptor.capture());
        assertThat(batchCaptor.getValue().getBatchId()).isNotBlank();
        assertThat(batchCaptor.getValue().getGeneratedAt()).isNotNull();
        assertThat(batchCaptor.getValue().getRecords()).containsExactly(event);
        assertThat(event.getDeliveryStatus()).isEqualTo("SENT");
        assertThat(event.getSentAt()).isNotNull();
        assertThat(event.getDeliveryError()).isNull();
        verify(repository).saveAll(List.of(event));
    }

    @Test
    void marksTheBatchFailedWhenLevel4RejectsIt() {
        Level4Client client = mock(Level4Client.class);
        TapEventRepository repository = mock(TapEventRepository.class);
        TapEvent event = event("event-1");
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
        TapEventRepository repository = mock(TapEventRepository.class);
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

    private ValidationRequest request(String ticketId) {
        ValidationRequest request = new ValidationRequest();
        long expiresAt = System.currentTimeMillis() / 1000 + 3600;
        request.setQrPayload("AFCQR:v1:" + ticketId + ":exp=" + expiresAt + ":hmac=placeholder");
        request.setEventType(TapEventType.CHECK_IN);
        return request;
    }

    private TapEvent event(String eventId) {
        TapEvent event = new TapEvent();
        event.setEventId(eventId);
        event.setTicketId("ticket-42");
        event.setGateId("gate-1");
        event.setStationId("station-1");
        event.setEventType(TapEventType.CHECK_OUT);
        event.setRecordedAt(LocalDateTime.of(2026, 6, 21, 15, 30));
        event.setDeliveryStatus("PENDING");
        return event;
    }
}
