package com.ticketapp.dto.external;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ExternalCardRequest {
    private String passengerAccountId;
    private String cardTypeCode;
    private String orderId;
    private String requestSource;
    private String paymentId;
    private String paymentMethod;
    private BigDecimal amount;
    private String currency;
    private String deliveryAddress;
}
