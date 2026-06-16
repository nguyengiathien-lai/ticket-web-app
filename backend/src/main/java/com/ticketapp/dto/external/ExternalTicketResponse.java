package com.ticketapp.dto.external;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ExternalTicketResponse {

    private String externalTicketId;
    private String passengerAccountId;
    private String ticketTypeCode;
    private String physicalCardExternalId;
    private String ticketCode;
    private String status;
    private BigDecimal fare;
    private String currency;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Integer remainingUses;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;
}
