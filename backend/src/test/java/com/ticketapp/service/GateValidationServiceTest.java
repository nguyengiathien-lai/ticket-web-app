package com.ticketapp.service;

import com.ticketapp.client.GateEventClient;
import com.ticketapp.dto.gate.GateEventType;
import com.ticketapp.dto.gate.ValidationRecordRequest;
import com.ticketapp.dto.gate.ValidationRecordResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GateValidationServiceTest {

    @Test
    void forwardsTheExactScanRecordAndReturnsTheHigherSystemMessage() {
        GateEventClient client = mock(GateEventClient.class);
        GateValidationService service = new GateValidationService(client);
        ValidationRecordRequest request = new ValidationRecordRequest();
        request.setTicketId(" ticket-42 ");
        request.setGateId(" gate-1 ");
        request.setStationId(" station-1 ");
        request.setEventType(GateEventType.CHECK_IN);
        request.setRecordedTime(LocalDateTime.of(2026, 6, 21, 15, 30));
        when(client.send(request)).thenReturn(new ValidationRecordResponse("Record received"));

        ValidationRecordResponse response = service.recordValidation(request);

        ArgumentCaptor<ValidationRecordRequest> captor =
                ArgumentCaptor.forClass(ValidationRecordRequest.class);
        verify(client).send(captor.capture());
        assertThat(captor.getValue().getTicketId()).isEqualTo("ticket-42");
        assertThat(captor.getValue().getGateId()).isEqualTo("gate-1");
        assertThat(captor.getValue().getStationId()).isEqualTo("station-1");
        assertThat(captor.getValue().getEventType()).isEqualTo(GateEventType.CHECK_IN);
        assertThat(captor.getValue().getRecordedTime())
                .isEqualTo(LocalDateTime.of(2026, 6, 21, 15, 30));
        assertThat(response.getMessage()).isEqualTo("Record received");
    }
}
