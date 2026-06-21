package com.ticketapp.client.level5;

import com.ticketapp.dto.external.ExternalCardRequest;
import com.ticketapp.dto.external.ExternalCardResponse;
import com.ticketapp.dto.external.ExternalTicketRequest;
import com.ticketapp.dto.external.ExternalTicketResponse;
import com.ticketapp.dto.external.PurchaseActivityRequest;
import com.ticketapp.dto.external.PurchaseActivityResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Component
public class ExternalLevel5Client implements Level5Client {

    private static final Map<String, TicketDefaults> TICKET_DEFAULTS = Map.of(
            "single_trip", new TicketDefaults(new BigDecimal("15000"), 1, 1),
            "route001", new TicketDefaults(new BigDecimal("15000"), 1, 1),
            "day_pass", new TicketDefaults(new BigDecimal("50000"), 1, 50),
            "weekly_pass", new TicketDefaults(new BigDecimal("200000"), 7, 250),
            "pkg001", new TicketDefaults(new BigDecimal("150000"), 30, 20),
            "pkg002", new TicketDefaults(new BigDecimal("300000"), 60, 50));

    private final RestClient restClient;
    private final String ticketRequestPath;
    private final String cardRequestPath;
    private final String purchaseActivityPath;
    private final boolean mockEnabled;

    public ExternalLevel5Client(
            RestClient.Builder builder,
            @Value("${app.level5.base-url:}") String baseUrl,
            @Value("${app.level5.ticket-request-path:/tickets/request}") String ticketRequestPath,
            @Value("${app.level5.card-request-path:/cards/request}") String cardRequestPath,
            @Value("${app.level5.purchase-activity-path:/purchase-activities}") String purchaseActivityPath,
            @Value("${app.level5.mock-enabled:true}") boolean mockEnabled) {
        this.restClient = baseUrl.isBlank() ? builder.build() : builder.baseUrl(baseUrl).build();
        this.ticketRequestPath = ticketRequestPath;
        this.cardRequestPath = cardRequestPath;
        this.purchaseActivityPath = purchaseActivityPath;
        this.mockEnabled = mockEnabled;
    }

    @Override
    public ExternalTicketResponse requestTicket(ExternalTicketRequest request) {
        if (mockEnabled) {
            return mockTicket(request);
        }
        return post(ticketRequestPath, request, ExternalTicketResponse.class, "ticket request");
    }

    @Override
    public ExternalCardResponse requestCard(ExternalCardRequest request) {
        if (mockEnabled) {
            LocalDateTime now = LocalDateTime.now();
            ExternalCardResponse response = new ExternalCardResponse();
            response.setExternalCardId("card_" + shortToken());
            response.setCardUid("UID-" + shortToken().toUpperCase());
            response.setMaskedCardNumber("**** **** **** " + digits(4));
            response.setStatus("INACTIVE");
            response.setIssuedAt(now);
            response.setExpiresAt(now.plusDays(30));
            return response;
        }
        return post(cardRequestPath, request, ExternalCardResponse.class, "card request");
    }

    @Override
    public PurchaseActivityResponse recordPurchase(PurchaseActivityRequest request) {
        if (mockEnabled) {
            PurchaseActivityResponse response = new PurchaseActivityResponse();
            response.setMessage("Purchase activity received successfully");
            return response;
        }
        return post(purchaseActivityPath, request, PurchaseActivityResponse.class, "purchase activity");
    }

    private ExternalTicketResponse mockTicket(ExternalTicketRequest request) {
        LocalDateTime now = LocalDateTime.now();
        TicketDefaults defaults = TICKET_DEFAULTS.getOrDefault(
                request.getTicketTypeCode(), new TicketDefaults(new BigDecimal("15000"), 1, 1));
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

    private <T> T post(String path, Object body, Class<T> responseType, String operation) {
        T response = restClient.post().uri(path).contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().body(responseType);
        if (response == null) {
            throw new IllegalStateException("Level 5 returned an empty response for " + operation);
        }
        return response;
    }

    private String shortToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String digits(int length) {
        String value = UUID.randomUUID().toString().replaceAll("\\D", "");
        return value.substring(0, Math.min(length, value.length()));
    }

    private record TicketDefaults(BigDecimal price, int validityDays, int remainingUses) {
    }
}
