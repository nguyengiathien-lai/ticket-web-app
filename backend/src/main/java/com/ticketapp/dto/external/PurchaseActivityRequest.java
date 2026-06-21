package com.ticketapp.dto.external;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PurchaseActivityRequest {
    private String orderId;
    private String activityType;
    private String passengerAccountId;
    private String purchasedItemId;
    private BigDecimal amount;
    private String currency;
    private String paymentId;
    private LocalDateTime occurredAt;
}
