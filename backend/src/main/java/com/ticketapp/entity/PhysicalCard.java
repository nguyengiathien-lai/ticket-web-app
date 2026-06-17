package com.ticketapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "physical_cards")
public class PhysicalCard extends BaseEntity {

    @Column(name = "external_card_id", nullable = false, unique = true, length = 100)
    private String externalCardId;

    @Column(name = "passenger_account_id", nullable = false, length = 36)
    private String passengerAccountId;

    @Column(name = "card_uid", nullable = false, unique = true, length = 100)
    private String cardUid;

    @Column(name = "masked_card_number", length = 30)
    private String maskedCardNumber;

    @Column(nullable = false, length = 20)
    private String status; // ACTIVE, INACTIVE, EXPIRED

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "cached_at", nullable = false)
    private LocalDateTime cachedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
