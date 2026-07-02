package com.ticketapp.entity;

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
public class Ticket {

    private String externalTicketId;

    private String passengerAccountId;

    private String ticketTypeCode;

    private String physicalCardExternalId;

    private String ticketCode;

    private String status; // ACTIVE, USED, EXPIRED, CANCELLED

    private String mode;

    private String scope;

    private BigDecimal fare;

    private String currency;

    private String fromStationCode;

    private String toStationCode;

    private LocalDateTime validFrom;

    private LocalDateTime validUntil;

    private String qrToken;

    private Boolean expired;

    private Integer remainingUses;

    private LocalDateTime issuedAt;

    private LocalDateTime cachedAt;

    private LocalDateTime expiresAt;
}
