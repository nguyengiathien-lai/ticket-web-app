package com.ticketapp.dto.purchase;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class TicketPurchaseResponse {

    private String ticketId;
    private String orderId;
    private String userId;
    private String ticketType;
    private String origin;
    private String destination;
    private BigDecimal totalPrice;
    private String currency;
    private String status;
    private String qrCode;
    private String confirmationNumber;
    private String paymentId;
    private String paymentStatus;
    private LocalDateTime purchasedAt;
}
