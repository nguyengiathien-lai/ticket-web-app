package com.validationgate.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ExternalGateEventRequest {
    private String eventId;
    private String ticketId;
    private TapEventType eventType;
    private String gateId;
    private String stationId;
    private LocalDateTime recordedAt;
    private String source;
}
