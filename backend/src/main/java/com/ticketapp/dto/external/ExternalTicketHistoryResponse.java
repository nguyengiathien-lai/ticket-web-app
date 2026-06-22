package com.ticketapp.dto.external;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class ExternalTicketHistoryResponse {

    @JsonAlias({"ticketID", "externalTicketId"})
    private String ticketId;

    @JsonAlias("ticketType")
    private String type;

    private String status;

    @JsonAlias("fare")
    private BigDecimal price;

    private LocalDate validFrom;

    @JsonAlias({"validUntil", "expiresAt"})
    private LocalDate validTo;

    @JsonAlias({"issuedAt", "createdAt"})
    private LocalDateTime purchasedAt;
}
