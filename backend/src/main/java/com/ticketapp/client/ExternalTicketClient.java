package com.ticketapp.client;

import com.ticketapp.dto.external.ExternalTicketRequest;
import com.ticketapp.dto.external.ExternalTicketResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Component
public class ExternalTicketClient {

    private static final Map<String, TicketDefaults> TICKET_DEFAULTS = Map.of(
            "single_trip", new TicketDefaults(new BigDecimal("15000"), 1, 1),
            "route001", new TicketDefaults(new BigDecimal("15000"), 1, 1),
            "day_pass", new TicketDefaults(new BigDecimal("50000"), 1, 50),
            "weekly_pass", new TicketDefaults(new BigDecimal("200000"), 7, 250),
            "pkg001", new TicketDefaults(new BigDecimal("150000"), 30, 20),
            "pkg002", new TicketDefaults(new BigDecimal("300000"), 60, 50));

    public ExternalTicketResponse requestTicket(ExternalTicketRequest request) {
        LocalDateTime now = LocalDateTime.now();
        TicketDefaults defaults = TICKET_DEFAULTS.getOrDefault(
                request.getTicketTypeCode(),
                new TicketDefaults(new BigDecimal("15000"), 1, 1));

        String token = shortToken();
        ExternalTicketResponse response = new ExternalTicketResponse();
        response.setExternalTicketId("ticket_" + token);
        response.setPassengerAccountId(request.getPassengerAccountId());
        response.setTicketTypeCode(request.getTicketTypeCode());
        response.setPhysicalCardExternalId(request.getPhysicalCardExternalId());
        response.setTicketCode("TICKET-" + token.toUpperCase());
        response.setStatus("ACTIVE");
        response.setFare(defaults.price());
        response.setCurrency("VND");
        response.setValidFrom(now);
        response.setValidUntil(now.plusDays(defaults.validityDays()));
        response.setRemainingUses(defaults.remainingUses());
        response.setIssuedAt(now);
        response.setExpiresAt(now.plusDays(defaults.validityDays()));
        return response;
    }

    private String shortToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private record TicketDefaults(BigDecimal price, int validityDays, int remainingUses) {
    }
}
