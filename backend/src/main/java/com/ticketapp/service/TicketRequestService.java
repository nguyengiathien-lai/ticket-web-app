package com.ticketapp.service;

import com.ticketapp.client.ExternalTicketClient;
import com.ticketapp.dto.external.ExternalTicketRequest;
import com.ticketapp.dto.external.ExternalTicketResponse;
import com.ticketapp.dto.ticket.TicketRequest;
import com.ticketapp.dto.ticket.TicketResponse;
import com.ticketapp.entity.Ticket;
import com.ticketapp.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TicketRequestService {

    private final AccountService accountService;
    private final ExternalTicketClient externalTicketClient;
    private final TicketRepository ticketRepository;

    public TicketRequestService(
            AccountService accountService,
            ExternalTicketClient externalTicketClient,
            TicketRepository ticketRepository) {
        this.accountService = accountService;
        this.externalTicketClient = externalTicketClient;
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public TicketResponse requestTicket(TicketRequest request) {
        accountService.findById(request.getPassengerAccountId())
                .filter(account -> account.getIsActive() && account.getIsEmailVerified())
                .orElseThrow(() -> new IllegalArgumentException("Passenger account not found, inactive, or unverified"));

        ExternalTicketResponse externalTicket = externalTicketClient.requestTicket(
                ExternalTicketRequest.builder()
                        .passengerAccountId(request.getPassengerAccountId())
                        .ticketTypeCode(request.getTicketTypeCode())
                        .physicalCardExternalId(request.getPhysicalCardExternalId())
                        .idempotencyKey(request.getIdempotencyKey())
                        .requestSource("TICKET_WEB_APP")
                        .build());

        if (externalTicket == null || externalTicket.getExternalTicketId() == null
                || externalTicket.getTicketCode() == null) {
            throw new IllegalStateException("External ticket system returned an incomplete ticket");
        }

        Ticket ticket = ticketRepository.findByExternalTicketId(externalTicket.getExternalTicketId())
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
        LocalDateTime now = LocalDateTime.now();

        ticket.setExternalTicketId(externalTicket.getExternalTicketId());
        ticket.setPassengerAccountId(coalesce(externalTicket.getPassengerAccountId(), request.getPassengerAccountId()));
        ticket.setTicketTypeCode(coalesce(externalTicket.getTicketTypeCode(), request.getTicketTypeCode()));
        ticket.setPhysicalCardExternalId(coalesce(
                externalTicket.getPhysicalCardExternalId(),
                request.getPhysicalCardExternalId()));
        ticket.setTicketCode(externalTicket.getTicketCode());
        ticket.setStatus(coalesce(externalTicket.getStatus(), "ACTIVE"));
        ticket.setFare(externalTicket.getFare());
        ticket.setCurrency(coalesce(externalTicket.getCurrency(), "VND"));
        ticket.setValidFrom(externalTicket.getValidFrom());
        ticket.setValidUntil(externalTicket.getValidUntil());
        ticket.setRemainingUses(externalTicket.getRemainingUses());
        ticket.setIssuedAt(coalesce(externalTicket.getIssuedAt(), now));
        ticket.setCachedAt(now);
        ticket.setExpiresAt(coalesce(externalTicket.getExpiresAt(), externalTicket.getValidUntil()));
    }

    private String coalesce(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private LocalDateTime coalesce(LocalDateTime first, LocalDateTime second) {
        return first == null ? second : first;
    }
}
