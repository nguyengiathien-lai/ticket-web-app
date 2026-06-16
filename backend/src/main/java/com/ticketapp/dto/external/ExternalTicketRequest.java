package com.ticketapp.dto.external;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExternalTicketRequest {

    private String passengerAccountId;
    private String ticketTypeCode;
    private String physicalCardExternalId;
    private String idempotencyKey;
    private String requestSource;
}
