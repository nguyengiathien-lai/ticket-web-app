package com.ticketapp.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PhysicalCard {

    private String externalCardId;

    private String passengerAccountId;

    private String cardUid;

    private String maskedCardNumber;

    private String status; // ACTIVE, INACTIVE, EXPIRED

    private LocalDateTime issuedAt;

    private LocalDateTime expiredAt;

    private LocalDateTime cachedAt;

    private LocalDateTime expiresAt;
}
