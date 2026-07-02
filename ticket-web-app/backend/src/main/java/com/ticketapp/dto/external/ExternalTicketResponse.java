package com.ticketapp.dto.external;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class ExternalTicketResponse {

    @JsonAlias({"ticketId", "ticketID"})
    private String externalTicketId;
    private String passengerAccountId;
    private String ticketTypeCode;
    private String physicalCardExternalId;
    private String ticketCode;
    private String status;
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
    private LocalDateTime expiresAt;
}
