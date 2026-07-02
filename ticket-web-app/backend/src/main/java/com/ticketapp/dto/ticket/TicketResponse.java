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

    public static TicketResponse from(Ticket ticket) {
        return TicketResponse.builder()
                .externalTicketId(ticket.getExternalTicketId())
                .passengerAccountId(ticket.getPassengerAccountId())
                .ticketTypeCode(ticket.getTicketTypeCode())
                .physicalCardExternalId(ticket.getPhysicalCardExternalId())
                .ticketCode(ticket.getTicketCode())
                .status(ticket.getStatus())
                .mode(ticket.getMode())
                .scope(ticket.getScope())
                .fare(ticket.getFare())
                .currency(ticket.getCurrency())
                .fromStationCode(ticket.getFromStationCode())
                .toStationCode(ticket.getToStationCode())
                .validFrom(ticket.getValidFrom())
                .validUntil(ticket.getValidUntil())
                .qrToken(ticket.getQrToken())
                .expired(ticket.getExpired())
                .remainingUses(ticket.getRemainingUses())
                .issuedAt(ticket.getIssuedAt())
                .cachedAt(ticket.getCachedAt())
                .expiresAt(ticket.getExpiresAt())
                .build();
    }
}
