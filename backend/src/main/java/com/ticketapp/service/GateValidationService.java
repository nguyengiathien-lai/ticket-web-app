package com.ticketapp.service;

import com.ticketapp.dto.gate.GateValidationRequest;
import com.ticketapp.dto.gate.GateValidationResponse;
import com.ticketapp.entity.Ticket;
import com.ticketapp.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class GateValidationService {

    private final TicketRepository ticketRepository;

    public GateValidationService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Transactional
    public GateValidationResponse validateTicket(GateValidationRequest request) {
        String normalizedTicketCode = request.getTicketCode().trim().toUpperCase();
        Ticket ticket = ticketRepository.findByTicketCode(normalizedTicketCode)
                .orElse(null);

        String status = ticket == null ? resolveStatus(normalizedTicketCode) : resolveCachedTicketStatus(ticket);
        String message = resolveMessage(status);

        if ("VALID".equals(status) && ticket != null && ticket.getRemainingUses() != null) {
            int remainingUses = Math.max(0, ticket.getRemainingUses() - 1);
            ticket.setRemainingUses(remainingUses);
            if (remainingUses == 0) {
                ticket.setStatus("USED");
            }
            ticketRepository.save(ticket);
        }

        return new GateValidationResponse(
                status,
                normalizedTicketCode,
                message,
                request.getGateId(),
                request.getStationId(),
                LocalDateTime.now()
        );
    }

    private String resolveCachedTicketStatus(Ticket ticket) {
        LocalDateTime now = LocalDateTime.now();

        if ("CANCELLED".equalsIgnoreCase(ticket.getStatus()) || "CANCELED".equalsIgnoreCase(ticket.getStatus())) {
            return "CANCELLED";
        }

        if ("USED".equalsIgnoreCase(ticket.getStatus())) {
            return "USED";
        }

        if ("EXPIRED".equalsIgnoreCase(ticket.getStatus())
                || (ticket.getValidUntil() != null && ticket.getValidUntil().isBefore(now))
                || (ticket.getExpiresAt() != null && ticket.getExpiresAt().isBefore(now))) {
            ticket.setStatus("EXPIRED");
            ticketRepository.save(ticket);
            return "EXPIRED";
        }

        if (ticket.getValidFrom() != null && ticket.getValidFrom().isAfter(now)) {
            return "NOT_YET_VALID";
        }

        if (ticket.getRemainingUses() != null && ticket.getRemainingUses() <= 0) {
            ticket.setStatus("USED");
            ticketRepository.save(ticket);
            return "USED";
        }

        if ("ACTIVE".equalsIgnoreCase(ticket.getStatus())) {
            return "VALID";
        }

        return "INVALID";
    }

    private String resolveStatus(String ticketCode) {
        if (ticketCode.startsWith("TICKET-") || ticketCode.startsWith("VALID-")) {
            return "VALID";
        }

        if (ticketCode.startsWith("USED-")) {
            return "USED";
        }

        if (ticketCode.startsWith("EXPIRED-")) {
            return "EXPIRED";
        }

        if (ticketCode.startsWith("CANCELLED-") || ticketCode.startsWith("CANCELED-")) {
            return "CANCELLED";
        }

        return "INVALID";
    }

    private String resolveMessage(String status) {
        return switch (status) {
            case "VALID" -> "Access granted";
            case "USED" -> "Ticket has already been used";
            case "EXPIRED" -> "Ticket has expired";
            case "CANCELLED" -> "Ticket has been cancelled";
            case "NOT_YET_VALID" -> "Ticket is not valid yet";
            default -> "Ticket was not recognized";
        };
    }
}
