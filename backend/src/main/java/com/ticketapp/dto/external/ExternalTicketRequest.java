package com.ticketapp.dto.external;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ExternalTicketRequest {

    private String passengerAccountId;
    private String ticketTypeCode;
    private String physicalCardExternalId;
    private String idempotencyKey;
    private String requestSource;
    private String orderId;
    private String paymentId;
    private String paymentMethod;
    private BigDecimal amount;
    private String currency;
}
