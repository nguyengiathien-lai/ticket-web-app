package com.ticketapp.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketRequest {

    @NotBlank
    private String passengerAccountId;

    @NotBlank
    private String ticketTypeCode;

    private String physicalCardExternalId;

    private String idempotencyKey;
}
