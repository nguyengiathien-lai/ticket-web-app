package com.ticketapp.dto.gate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ValidationRecordRequest {

    @NotBlank
    private String ticketId;

    @NotBlank
    private String gateId;

    @NotBlank
    private String stationId;

    @NotNull
    private GateEventType eventType;

    @NotNull
    private LocalDateTime recordedTime;
}
