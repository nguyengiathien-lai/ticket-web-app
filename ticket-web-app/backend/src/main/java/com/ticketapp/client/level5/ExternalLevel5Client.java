package com.ticketapp.client.level5;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketapp.dto.external.ExternalCardHistoryResponse;
import com.ticketapp.dto.external.ExternalDiscountResponse;
import com.ticketapp.dto.external.ExternalFarePriceResponse;
import com.ticketapp.dto.external.ExternalPassTicketRequest;
import com.ticketapp.dto.external.ExternalPassengerRouteResponse;
import com.ticketapp.dto.external.ExternalPassengerStationResponse;
import com.ticketapp.dto.external.ExternalSingleTripTicketRequest;
import com.ticketapp.dto.external.ExternalTicketRequest;
import com.ticketapp.dto.external.ExternalTicketResponse;
import com.ticketapp.dto.external.ExternalTicketHistoryResponse;
import com.ticketapp.dto.external.ExternalTravelHistoryResponse;
import com.ticketapp.dto.purchase.CardPurchaseRequest;
import com.ticketapp.dto.purchase.CardPurchaseResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    private final ObjectMapper objectMapper;
    private final String ticketPurchasePath;
    private final String singleTripTicketPurchasePath;
    private final String passTicketPurchasePath;
    private final String cardPurchasePath;
    private final String passengerStationsPath;
    private final String passengerRoutesPath;
    private final String passengerCardsPath;
    private final String passengerTicketsPath;
    private final String passengerTripsPath;
    private final String farePricesPath;
    private final String fareDiscountsPath;
    private final boolean mockEnabled;

    public ExternalLevel5Client(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${app.level5.base-url:http://localhost:8082}") String baseUrl,
            @Value("${app.level5.ticket-purchase-path:/api/tickets/purchase}") String ticketPurchasePath,
            @Value("${app.level5.single-trip-ticket-purchase-path:/api/tickets/single-trip}") String singleTripTicketPurchasePath,
            @Value("${app.level5.pass-ticket-purchase-path:/api/tickets/pass}") String passTicketPurchasePath,
            @Value("${app.level5.card-purchase-path:/api/cards/purchase}") String cardPurchasePath,
            @Value("${app.level5.passenger-stations-path:/api/passenger/stations}") String passengerStationsPath,
            @Value("${app.level5.passenger-routes-path:/api/passenger/routes}") String passengerRoutesPath,
            @Value("${app.level5.passenger-cards-path:/api/passengers/{userId}/cards}") String passengerCardsPath,
            @Value("${app.level5.passenger-tickets-path:/api/passengers/{userId}/tickets}") String passengerTicketsPath,
            @Value("${app.level5.passenger-trips-path:/api/passengers/{userId}/trips}") String passengerTripsPath,
            @Value("${app.level5.fare-prices-path:/api/passenger/fare/prices}") String farePricesPath,
            @Value("${app.level5.fare-discounts-path:/api/passenger/fare/discounts}") String fareDiscountsPath,
            @Value("${app.level5.mock-enabled:false}") boolean mockEnabled) {
        this.restClient = baseUrl.isBlank() ? builder.build() : builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.ticketPurchasePath = ticketPurchasePath;
        this.singleTripTicketPurchasePath = singleTripTicketPurchasePath;
        this.passTicketPurchasePath = passTicketPurchasePath;
        this.cardPurchasePath = cardPurchasePath;
        this.passengerStationsPath = passengerStationsPath;
        this.passengerRoutesPath = passengerRoutesPath;
        this.passengerCardsPath = passengerCardsPath;
        this.passengerTicketsPath = passengerTicketsPath;
        this.passengerTripsPath = passengerTripsPath;
        this.farePricesPath = farePricesPath;
        this.fareDiscountsPath = fareDiscountsPath;
        this.mockEnabled = mockEnabled;
    }

    // @Override
    // public ExternalTicketResponse purchaseTicket(ExternalTicketRequest request) {
    //     if (mockEnabled) {
    //         return mockTicket(request);
    //     }
    //     return post(ticketPurchasePath, request, ExternalTicketResponse.class, "ticket purchase");
    // }

    @Override
    public ExternalTicketResponse purchaseSingleTripTicket(ExternalSingleTripTicketRequest request) {
        if (mockEnabled) {
            return mockSingleTripTicket(request);
        }
        return post(
                singleTripTicketPurchasePath,
                request,
                ExternalTicketResponse.class,
                "single-trip ticket purchase");
    }

    @Override
    public ExternalTicketResponse purchasePassTicket(ExternalPassTicketRequest request) {
        if (mockEnabled) {
            return mockPassTicket(request);
        }
        return post(passTicketPurchasePath, request, ExternalTicketResponse.class, "pass ticket purchase");
    }

    @Override
    public CardPurchaseResponse purchaseCard(CardPurchaseRequest request) {
        if (mockEnabled) {
            LocalDateTime now = LocalDateTime.now();
            LocalDate validFrom = request.getTicket() == null || request.getTicket().getValidFrom() == null
                    ? LocalDate.now()
                    : request.getTicket().getValidFrom();
            int months = request.getTicket() == null || request.getTicket().getDurationMonths() == null
                    ? 1
                    : Math.max(request.getTicket().getDurationMonths(), 1);
            String userId = coalesce(
                    request.getCard() == null ? null : request.getCard().getUserId(),
                    request.getTicket() == null ? null : request.getTicket().getUserId());
            String mode = coalesce(request.getTicket() == null ? null : request.getTicket().getMode(), "METRO");
            String cardId = "card_" + shortToken();
            return CardPurchaseResponse.builder()
                    .card(CardPurchaseResponse.IssuedCard.builder()
                            .id(cardId)
                            .cardUid(coalesce(
                                    request.getCard() == null ? null : request.getCard().getCardUid(),
                                    "UID-" + shortToken().toUpperCase()))
                            .status("ACTIVE")
                            .type("MONTHLY_PASS")
                            .supportsMetro(firstValue(
                                    request.getCard() == null ? null : request.getCard().getSupportsMetro(),
                                    "METRO".equalsIgnoreCase(mode)))
                            .supportsBus(firstValue(
                                    request.getCard() == null ? null : request.getCard().getSupportsBus(),
                                    "BUS".equalsIgnoreCase(mode)))
                            .linkedUserId(userId)
                            .activatedAt(now)
                            .createdAt(now)
                            .updatedAt(now)
                            .build())
                    .ticket(CardPurchaseResponse.IssuedTicket.builder()
                            .ticketId("ticket_" + shortToken())
                            .type("MONTHLY_PASS")
                            .mode(mode)
                            .scope(coalesce(request.getTicket() == null ? null : request.getTicket().getScope(), "SINGLE_ROUTE"))
                            .routeId(request.getTicket() == null ? null : request.getTicket().getRouteId())
                            .status("ACTIVE")
                            .cardId(cardId)
                            .userId(userId)
                            .price(new BigDecimal("200000").multiply(BigDecimal.valueOf(months)))
                            .validFrom(validFrom)
                            .validTo(validFrom.plusMonths(months).minusDays(1))
                            .purchasedAt(now)
                            .build())
                    .build();
        }
        return post(cardPurchasePath, request, CardPurchaseResponse.class, "card purchase");
    }

    @Override
    public List<ExternalCardHistoryResponse> getCards(String userId) {
        if (mockEnabled) {
            Instant now = Instant.now();
            ExternalCardHistoryResponse response = new ExternalCardHistoryResponse();
            response.setCardId(UUID.randomUUID().toString());
            response.setCardUid("UID-" + shortToken().toUpperCase());
            response.setStatus("ACTIVE");
            response.setType("STANDARD");
            response.setActivatedAt(toLocalDateTime(now));
            response.setLinkedAt(toLocalDateTime(now));
            return List.of(response);
        }
        return Arrays.asList(get(
                passengerCardsPath, "userId", userId, ExternalCardHistoryResponse[].class, "passenger cards"));
    }

    @Override
    public List<ExternalTicketHistoryResponse> getTickets(String userId) {
        if (mockEnabled) {
            UUID ticketId = UUID.randomUUID();
            LocalDate today = LocalDate.now();
            ExternalTicketHistoryResponse response = new ExternalTicketHistoryResponse();
            response.setTicketId(ticketId.toString());
            response.setType("SINGLE_TRIP");
            response.setMode("METRO");
            response.setStatus("ACTIVE");
            response.setPrice(new BigDecimal("15000"));
            response.setValidFrom(today);
            response.setValidTo(today.plusDays(1));
            response.setQrToken(ticketId.toString());
            response.setExpired(false);
            response.setPurchasedAt(toLocalDateTime(Instant.now()));
            return List.of(response);
        }
        return Arrays.asList(get(
                passengerTicketsPath, "userId", userId, ExternalTicketHistoryResponse[].class, "passenger tickets"));
    }

    @Override
    public List<ExternalTravelHistoryResponse> getTravelHistory(String userId) {
        if (mockEnabled) {
            Instant now = Instant.now();
            ExternalTravelHistoryResponse response = new ExternalTravelHistoryResponse();
            response.setExternalTripId(UUID.randomUUID().toString());
            response.setPassengerAccountId(userId);
            response.setTicketExternalId(UUID.randomUUID().toString());
            response.setMode("METRO");
            response.setCheckinStationCode("CL");
            response.setCheckoutStationCode("HD");
            response.setCheckinTime(toLocalDateTime(now.minusSeconds(1800)));
            response.setCheckoutTime(toLocalDateTime(now));
            response.setDistanceKm(new BigDecimal("8.5"));
            response.setFareAmount(new BigDecimal("15000"));
            return List.of(response);
        }
        return Arrays.asList(get(
                passengerTripsPath, "userId", userId, ExternalTravelHistoryResponse[].class, "passenger trips"));
    }

    @Override
    public List<ExternalPassengerStationResponse> getStations() {
        if (mockEnabled) {
            Instant now = Instant.now();
            return List.of(
                    new ExternalPassengerStationResponse(
                            UUID.randomUUID(), UUID.randomUUID(), "HN_2A_01", "Cat Linh",
                            BigDecimal.ZERO, 1, now),
                    new ExternalPassengerStationResponse(
                            UUID.randomUUID(), UUID.randomUUID(), "HN_2A_02", "La Thanh",
                            new BigDecimal("0.700"), 2, now));
        }
        return Arrays.asList(get(
                passengerStationsPath, ExternalPassengerStationResponse[].class, "passenger stations"));
    }

    @Override
    public List<ExternalPassengerRouteResponse> getRoutes() {
        if (mockEnabled) {
            Instant now = Instant.now();
            return List.of(
                    new ExternalPassengerRouteResponse(
                            UUID.randomUUID(), UUID.randomUUID(), "HN_2A", "Cat Linh - Ha Dong", "METRO", now),
                    new ExternalPassengerRouteResponse(
                            UUID.randomUUID(), UUID.randomUUID(), "HN_BUS_32", "Bus 32: Giap Bat - Nhon", "BUS", now));
        }
        return Arrays.asList(get(
                passengerRoutesPath, ExternalPassengerRouteResponse[].class, "passenger routes"));
    }


    @Override
    public List<ExternalFarePriceResponse> getFarePrices() {
        if (mockEnabled) {
            return mockFarePrices();
        }
        return Arrays.asList(get(
                farePricesPath, ExternalFarePriceResponse[].class, "fare prices"));
    }

    @Override
    public List<ExternalDiscountResponse> getFareDiscounts() {
        if (mockEnabled) {
            return List.of(new ExternalDiscountResponse(
                    "STUDENT", "PERCENTAGE", new BigDecimal("50"), LocalDate.now(), null));
        }
        return Arrays.asList(get(
                fareDiscountsPath, ExternalDiscountResponse[].class, "fare discounts"));
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

    private ExternalTicketResponse mockSingleTripTicket(ExternalSingleTripTicketRequest request) {
        LocalDate today = LocalDate.now();
        String token = shortToken();
        ExternalTicketResponse response = new ExternalTicketResponse();
        response.setExternalTicketId("ticket_" + token);
        response.setTicketTypeCode("SINGLE_TRIP");
        response.setPassengerAccountId(request.getUserId());
        response.setMode(request.getMode());
        response.setStatus("ACTIVE");
        response.setFare(new BigDecimal("15000"));
        response.setCurrency("VND");
        response.setFromStationCode(coalesce(request.getFromStationId(), "FROM"));
        response.setToStationCode(coalesce(request.getToStationId(), "TO"));
        response.setValidFrom(today.atStartOfDay());
        response.setValidUntil(today.atTime(23, 59, 59));
        response.setIssuedAt(LocalDateTime.now());
        response.setExpiresAt(response.getValidUntil());
        response.setQrToken(response.getExternalTicketId());
        response.setExpired(false);
        return response;
    }

    private ExternalTicketResponse mockPassTicket(ExternalPassTicketRequest request) {
        LocalDate validFrom = request.getValidFrom() == null ? LocalDate.now() : request.getValidFrom();
        int months = request.getDurationMonths() == null ? 1 : Math.max(request.getDurationMonths(), 1);
        String token = shortToken();
        ExternalTicketResponse response = new ExternalTicketResponse();
        response.setExternalTicketId("ticket_" + token);
        response.setTicketTypeCode("MONTHLY_PASS");
        response.setPassengerAccountId(request.getUserId());
        response.setMode(request.getMode());
        response.setScope(request.getScope());
        response.setRouteId(request.getRouteId());
        response.setPhysicalCardExternalId(request.getCardId());
        response.setStatus("ACTIVE");
        response.setFare(new BigDecimal("200000").multiply(BigDecimal.valueOf(months)));
        response.setCurrency("VND");
        response.setValidFrom(validFrom.atStartOfDay());
        response.setValidUntil(validFrom.plusMonths(months).minusDays(1).atTime(23, 59, 59));
        response.setIssuedAt(LocalDateTime.now());
        response.setExpiresAt(response.getValidUntil());
        response.setQrToken(response.getExternalTicketId());
        response.setExpired(false);
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

    private <T> T get(String path, String variableName, String variableValue, Class<T> responseType, String operation) {
        byte[] response = restClient.get().uri(path, Map.of(variableName, variableValue)).retrieve().body(byte[].class);
        if (response == null || response.length == 0) {
            throw new IllegalStateException("Level 5 returned an empty response for " + operation);
        }
        return readJson(response, responseType, operation);
    }

    private <T> T get(String path, Class<T> responseType, String operation) {
        byte[] response = restClient.get().uri(path).retrieve().body(byte[].class);
        if (response == null || response.length == 0) {
            throw new IllegalStateException("Level 5 returned an empty response for " + operation);
        }
        return readJson(response, responseType, operation);
    }

    private <T> T readJson(byte[] response, Class<T> responseType, String operation) {
        try {
            return objectMapper.readValue(response, responseType);
        } catch (IOException exception) {
            throw new IllegalStateException("Level 5 returned invalid JSON for " + operation, exception);
        }
    }

    private String shortToken() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String coalesce(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private String coalesce(String first, String second, String third) {
        return coalesce(coalesce(first, second), third);
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

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private List<ExternalFarePriceResponse> mockFarePrices() {
        return List.of(
                new ExternalFarePriceResponse(
                        "METRO",
                        new ExternalFarePriceResponse.SingleTripPrice(
                                new BigDecimal("8000"), new BigDecimal("850"), new BigDecimal("8000"), new BigDecimal("30000")),
                        List.of(
                                new ExternalFarePriceResponse.PassPriceItem("DAILY", null, null, new BigDecimal("40000")),
                                new ExternalFarePriceResponse.PassPriceItem("WEEKLY", null, null, new BigDecimal("160000")),
                                new ExternalFarePriceResponse.PassPriceItem("MONTHLY", 1, null, new BigDecimal("200000")))),
                new ExternalFarePriceResponse(
                        "BUS",
                        new ExternalFarePriceResponse.SingleTripPrice(
                                new BigDecimal("3000"), new BigDecimal("450"), new BigDecimal("3000"), new BigDecimal("30000")),
                        List.of(
                                new ExternalFarePriceResponse.PassPriceItem("DAILY", null, null, new BigDecimal("30000")),
                                new ExternalFarePriceResponse.PassPriceItem("WEEKLY", null, null, new BigDecimal("120000")),
                                new ExternalFarePriceResponse.PassPriceItem("MONTHLY", 1, "SINGLE_ROUTE", new BigDecimal("140000")),
                                new ExternalFarePriceResponse.PassPriceItem("MONTHLY", 1, "MULTI_ROUTE", new BigDecimal("280000")))));
    }

    private record TicketDefaults(BigDecimal price, int validityDays, int remainingUses) {
    }
}
