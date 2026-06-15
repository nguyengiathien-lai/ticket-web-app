package com.ticketapp.service;

import com.ticketapp.dto.gate.GateValidationRequest;
import com.ticketapp.dto.gate.GateValidationResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class GateValidationService {

    public GateValidationResponse validateTicket(GateValidationRequest request) {
        String normalizedTicketCode = request.getTicketCode().trim().toUpperCase();
        String status = resolveStatus(normalizedTicketCode);
        String message = resolveMessage(status);

        return new GateValidationResponse(
                status,
                normalizedTicketCode,
                message,
                request.getGateId(),
                request.getStationId(),
                LocalDateTime.now()
        );
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
            default -> "Ticket was not recognized";
        };
    }
}
