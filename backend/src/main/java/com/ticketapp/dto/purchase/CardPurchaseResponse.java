package com.ticketapp.dto.purchase;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class CardPurchaseResponse {

    private String orderId;
    private String userId;
    private String cardId;
    private String cardUid;
    private String maskedCardNumber;
    private String packageId;
    private String status;
    private String deliveryAddress;
    private LocalDate estimatedDelivery;
    private BigDecimal totalPrice;
    private String currency;
    private String paymentId;
    private String paymentStatus;
    private LocalDateTime createdAt;
}
