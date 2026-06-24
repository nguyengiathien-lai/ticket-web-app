package com.ticketapp.dto.ticket;

import com.ticketapp.entity.Ticket;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class TicketResponse {

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
    private LocalDateTime cachedAt;
    private LocalDateTime expiresAt;

    public static TicketResponse from(Ticket ticket) {
        return TicketResponse.builder()
                .externalTicketId(ticket.getExternalTicketId())
                .passengerAccountId(ticket.getPassengerAccountId())
                .ticketTypeCode(ticket.getTicketTypeCode())
                .physicalCardExternalId(ticket.getPhysicalCardExternalId())
                .ticketCode(ticket.getTicketCode())
                .status(ticket.getStatus())
                .fare(ticket.getFare())
                .currency(ticket.getCurrency())
                .validFrom(ticket.getValidFrom())
                .validUntil(ticket.getValidUntil())
                .remainingUses(ticket.getRemainingUses())
                .issuedAt(ticket.getIssuedAt())
                .cachedAt(ticket.getCachedAt())
                .expiresAt(ticket.getExpiresAt())
                .build();
    }
}
