package com.ticketapp.dto.gate;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GateValidationRequest {

    @NotBlank
    private String ticketCode;

    @NotBlank
    private String gateId;

    @NotBlank
    private String stationId;
}
