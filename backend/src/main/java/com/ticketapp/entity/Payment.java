package com.ticketapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {

    @Column(name = "external_payment_id", nullable = false, unique = true, length = 100)
    private String externalPaymentId;

    @Column(name = "external_order_id", nullable = false, length = 100)
    private String externalOrderId;

    @Column(name = "passenger_account_id", nullable = false, length = 36)
    private String passengerAccountId;

    @Column(name = "payment_method", nullable = false, length = 30)
    private String paymentMethod; // CARD, WALLET, UPI, BANK_TRANSFER, CASH

    @Column(name = "transaction_code", nullable = false, unique = true, length = 100)
    private String transactionCode;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 20)
    private String status; // PENDING, COMPLETED, FAILED, REFUNDED

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "cached_at", nullable = false)
    private LocalDateTime cachedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
