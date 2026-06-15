package com.ticketapp.dto.gate;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class GateValidationResponse {

    private String status;
    private String ticketCode;
    private String message;
    private String gateId;
    private String stationId;
    private LocalDateTime validatedAt;
}
