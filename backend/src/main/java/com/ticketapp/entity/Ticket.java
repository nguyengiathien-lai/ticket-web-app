package com.ticketapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
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
@Table(
        name = "tickets",
        indexes = {
                @Index(name = "idx_tickets_passenger_issued", columnList = "passenger_account_id, issued_at"),
                @Index(name = "idx_tickets_physical_card", columnList = "physical_card_external_id")
        })
public class Ticket extends BaseEntity {

    @Column(name = "external_ticket_id", nullable = false, unique = true, length = 100)
    private String externalTicketId;

    @Column(name = "passenger_account_id", nullable = false, length = 36)
    private String passengerAccountId;

    @Column(name = "ticket_type_code", nullable = false, length = 50)
    private String ticketTypeCode;

    @Column(name = "physical_card_external_id", length = 100)
    private String physicalCardExternalId;

    @Column(name = "ticket_code", nullable = false, unique = true, length = 120)
    private String ticketCode;

    @Column(nullable = false, length = 20)
    private String status; // ACTIVE, USED, EXPIRED, CANCELLED

    @Column(precision = 12, scale = 2)
    private BigDecimal fare;

    @Column(length = 3)
    private String currency;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "remaining_uses")
    private Integer remainingUses;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "cached_at", nullable = false)
    private LocalDateTime cachedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
