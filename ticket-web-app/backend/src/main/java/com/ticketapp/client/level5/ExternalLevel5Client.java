package com.ticketapp.client.level5;

import com.ticketapp.dto.external.ExternalCardRequest;
import com.ticketapp.dto.external.ExternalCardResponse;
import com.ticketapp.dto.external.ExternalCardHistoryResponse;
import com.ticketapp.dto.external.ExternalCardTypeResponse;
import com.ticketapp.dto.external.ExternalTicketRequest;
import com.ticketapp.dto.external.ExternalTicketResponse;
import com.ticketapp.dto.external.ExternalTicketHistoryResponse;
import com.ticketapp.dto.external.ExternalTicketTypeResponse;
import com.ticketapp.dto.external.ExternalTravelHistoryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Arrays;
import java.util.List;
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
    private final String ticketPurchasePath;
    private final String cardPurchasePath;
    private final String ticketHistoryPath;
    private final String cardHistoryPath;
    private final String travelHistoryPath;
    private final String ticketTypesPath;
    private final String cardTypesPath;
    private final boolean mockEnabled;

    public ExternalLevel5Client(
            RestClient.Builder builder,
            @Value("${app.level5.base-url:}") String baseUrl,
            @Value("${app.level5.ticket-purchase-path:/api/ticket/purchase}") String ticketPurchasePath,
            @Value("${app.level5.card-purchase-path:/api/card/purchase}") String cardPurchasePath,
            @Value("${app.level5.ticket-history-path:/api/{accountId}/tickets}") String ticketHistoryPath,
            @Value("${app.level5.card-history-path:/api/{accountId}/cards}") String cardHistoryPath,
            @Value("${app.level5.travel-history-path:/api/{accountId}/travels}") String travelHistoryPath,
            @Value("${app.level5.ticket-types-path:/api/ticket-types}") String ticketTypesPath,
            @Value("${app.level5.card-types-path:/api/card-types}") String cardTypesPath,
            @Value("${app.level5.mock-enabled:true}") boolean mockEnabled) {
        this.restClient = baseUrl.isBlank() ? builder.build() : builder.baseUrl(baseUrl).build();
        this.ticketPurchasePath = ticketPurchasePath;
        this.cardPurchasePath = cardPurchasePath;
        this.ticketHistoryPath = ticketHistoryPath;
        this.cardHistoryPath = cardHistoryPath;
        this.travelHistoryPath = travelHistoryPath;
        this.ticketTypesPath = ticketTypesPath;
        this.cardTypesPath = cardTypesPath;
        this.mockEnabled = mockEnabled;
    }

    @Override
    public ExternalTicketResponse purchaseTicket(ExternalTicketRequest request) {
        if (mockEnabled) {
            return mockTicket(request);
        }
        return post(ticketPurchasePath, request, ExternalTicketResponse.class, "ticket purchase");
    }

    @Override
    public ExternalCardResponse purchaseCard(ExternalCardRequest request) {
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
        return post(cardPurchasePath, request, ExternalCardResponse.class, "card purchase");
    }

    @Override
    public List<ExternalTicketHistoryResponse> getTickets(String accountId) {
        if (mockEnabled) {
            return List.of();
        }
        return Arrays.asList(get(
                ticketHistoryPath, accountId, ExternalTicketHistoryResponse[].class, "ticket history"));
    }

    @Override
    public List<ExternalCardHistoryResponse> getCards(String accountId) {
        if (mockEnabled) {
            return List.of();
        }
        return Arrays.asList(get(
                cardHistoryPath, accountId, ExternalCardHistoryResponse[].class, "card history"));
    }

    @Override
    public List<ExternalTravelHistoryResponse> getTravelHistory(String accountId) {
        if (mockEnabled) {
            return List.of();
        }
        return Arrays.asList(get(
                travelHistoryPath, accountId, ExternalTravelHistoryResponse[].class, "travel history"));
    }

    @Override
    public List<ExternalTicketTypeResponse> getTicketTypes() {
        if (mockEnabled) {
            return List.of();
        }
        return Arrays.asList(get(
                ticketTypesPath, ExternalTicketTypeResponse[].class, "ticket type catalog"));
    }

    @Override
    public List<ExternalCardTypeResponse> getCardTypes() {
        if (mockEnabled) {
            return List.of();
        }
        return Arrays.asList(get(
                cardTypesPath, ExternalCardTypeResponse[].class, "card type catalog"));
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

    private <T> T get(String path, String accountId, Class<T> responseType, String operation) {
        T response = restClient.get().uri(path, accountId).retrieve().body(responseType);
        if (response == null) {
            throw new IllegalStateException("Level 5 returned an empty response for " + operation);
        }
        return response;
    }

    private <T> T get(String path, Class<T> responseType, String operation) {
        T response = restClient.get().uri(path).retrieve().body(responseType);
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
