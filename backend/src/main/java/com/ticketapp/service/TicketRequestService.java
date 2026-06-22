package com.ticketapp.service;

import com.ticketapp.dto.external.ExternalTicketResponse;
import com.ticketapp.dto.ticket.TicketRequest;
import com.ticketapp.dto.ticket.TicketResponse;
import com.ticketapp.entity.Ticket;
import com.ticketapp.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TicketRequestService {

    private final TicketRepository ticketRepository;

    public TicketRequestService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public TicketResponse cacheExternalTicket(TicketRequest request, ExternalTicketResponse externalTicket) {
        if (request == null || externalTicket == null) {
            throw new IllegalStateException("External ticket system returned an incomplete ticket");
        }
        String externalTicketId = requireText(externalTicket.getExternalTicketId(), "ticket ID");

        Ticket ticket = ticketRepository.findByExternalTicketId(externalTicketId)
                .orElseGet(Ticket::new);

        applyExternalTicket(ticket, request, externalTicket);

        return TicketResponse.from(ticketRepository.save(ticket));
    }

    public TicketResponse getCachedTicketByCode(String ticketCode) {
        return ticketRepository.findByTicketCode(ticketCode)
                .map(TicketResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> getCachedTicketsForPassenger(String passengerAccountId) {
        return ticketRepository.findByPassengerAccountIdOrderByIssuedAtDesc(passengerAccountId)
                .stream()
                .map(TicketResponse::from)
                .toList();
    }

    private void applyExternalTicket(
            Ticket ticket,
            TicketRequest request,
            ExternalTicketResponse externalTicket) {
        if (ticket == null || request == null || externalTicket == null) {
            throw new IllegalArgumentException("Ticket mapping inputs must not be null");
        }

        LocalDateTime now = LocalDateTime.now();

        String externalTicketId = requireText(externalTicket.getExternalTicketId(), "ticket ID");
        String passengerAccountId = requireText(firstText(
                externalTicket.getPassengerAccountId(),
                request.getPassengerAccountId(),
                ticket.getPassengerId()), "passenger account ID");
        String ticketTypeCode = requireText(firstText(
                externalTicket.getTicketTypeCode(),
                request.getTicketTypeCode(),
                ticket.getTicketType()), "ticket type code");

        BigDecimal fare = firstValue(externalTicket.getFare(), ticket.getFare());
        // Integer remainingUses = firstValue(externalTicket.getRemainingUses(), ticket.getRemainingUses());
        LocalDateTime validFrom = firstValue(externalTicket.getValidFrom(), ticket.getValidFrom());
        LocalDateTime validUntil = firstValue(externalTicket.getValidUntil(), ticket.getValidUntil());

        if (fare != null && fare.signum() < 0) {
            throw new IllegalStateException("Level 5 returned a negative ticket fare");
        }
        // if (remainingUses != null && remainingUses < 0) {
        //     throw new IllegalStateException("Level 5 returned negative remaining uses");
        // }
        // if (validFrom != null && validUntil != null && validUntil.isBefore(validFrom)) {
        //     throw new IllegalStateException("Level 5 returned an invalid ticket validity period");
        // }

        ticket.setTicketId(externalTicketId);
        ticket.setPassengerId(passengerAccountId);
        ticket.setTicketType(ticketTypeCode);
        ticket.setPhysicalCardExternalId(firstText(
                externalTicket.getPhysicalCardExternalId(),
                request.getPhysicalCardExternalId(),
                ticket.getPhysicalCardExternalId()));
        ticket.setTicketCode(firstText(externalTicket.getTicketCode(), ticket.getTicketCode(), externalTicketId));
        ticket.setStatus(firstText(externalTicket.getStatus(), ticket.getStatus(), "ACTIVE"));
        ticket.setFare(fare);
        ticket.setCurrency(firstText(externalTicket.getCurrency(), ticket.getCurrency(), "VND"));
        ticket.setValidFrom(validFrom);
        ticket.setValidUntil(validUntil);
        ticket.setIssuedAt(firstValue(externalTicket.getIssuedAt(), ticket.getIssuedAt(), now));
        ticket.setCachedAt(now);
        ticket.setExpiresAt(firstValue(externalTicket.getExpiresAt(), ticket.getExpiresAt(), validUntil));
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing " + fieldName + " in ticket data");
        }
        return value.trim();
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    @SafeVarargs
    private final <T> T firstValue(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
