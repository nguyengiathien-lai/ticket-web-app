package com.ticketapp.dto.external;

import com.ticketapp.dto.gate.GateEventType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ExternalGateEventRequest {
    private String eventId;
    private String ticketId;
    private GateEventType eventType;
    private String gateId;
    private String stationId;
    private LocalDateTime recordedAt;
    private String source;
}
