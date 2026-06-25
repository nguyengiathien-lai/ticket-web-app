package com.validationgate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidationRequest {

    @NotBlank
    private String ticketId;

    @NotBlank
    private String gateId;

    @NotBlank
    private String stationId;

    @NotNull
    private GateEventType eventType;
}
