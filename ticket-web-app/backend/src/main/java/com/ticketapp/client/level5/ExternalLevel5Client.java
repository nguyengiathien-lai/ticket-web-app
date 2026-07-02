package com.ticketapp.client.level5;

import com.ticketapp.dto.external.ExternalCardRequest;
import com.ticketapp.dto.external.ExternalCardResponse;
import com.ticketapp.dto.external.ExternalCardHistoryResponse;
import com.ticketapp.dto.external.ExternalCardTypeResponse;
import com.ticketapp.dto.external.ExternalDiscountResponse;
import com.ticketapp.dto.external.ExternalFarePriceResponse;
import com.ticketapp.dto.external.ExternalPassengerCardResponse;
import com.ticketapp.dto.external.ExternalPassengerRouteResponse;
import com.ticketapp.dto.external.ExternalPassengerTicketResponse;
import com.ticketapp.dto.external.ExternalPassengerTripResponse;
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
    private final String ticketPurchasePath;
    private final String cardPurchasePath;
    private final String ticketTypesPath;
    private final String cardTypesPath;
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
            @Value("${app.level5.base-url:http://localhost:8082}") String baseUrl,
            @Value("${app.level5.ticket-purchase-path:/api/tickets/purchase}") String ticketPurchasePath,
            @Value("${app.level5.card-purchase-path:/api/cards/purchase}") String cardPurchasePath,
            @Value("${app.level5.ticket-types-path:/api/tickets/types}") String ticketTypesPath,
            @Value("${app.level5.card-types-path:/api/cards/packages}") String cardTypesPath,
            @Value("${app.level5.passenger-stations-path:/api/passenger/stations}") String passengerStationsPath,
            @Value("${app.level5.passenger-routes-path:/api/passenger/routes}") String passengerRoutesPath,
            @Value("${app.level5.passenger-cards-path:/api/passengers/{userId}/cards}") String passengerCardsPath,
            @Value("${app.level5.passenger-tickets-path:/api/passengers/{userId}/tickets}") String passengerTicketsPath,
            @Value("${app.level5.passenger-trips-path:/api/passengers/{userId}/trips}") String passengerTripsPath,
            @Value("${app.level5.fare-prices-path:/api/passenger/fare/prices}") String farePricesPath,
            @Value("${app.level5.fare-discounts-path:/api/passenger/fare/discounts}") String fareDiscountsPath,
            @Value("${app.level5.mock-enabled:false}") boolean mockEnabled) {
        this.restClient = baseUrl.isBlank() ? builder.build() : builder.baseUrl(baseUrl).build();
        this.ticketPurchasePath = ticketPurchasePath;
        this.cardPurchasePath = cardPurchasePath;
        this.ticketTypesPath = ticketTypesPath;
        this.cardTypesPath = cardTypesPath;
        this.passengerStationsPath = passengerStationsPath;
        this.passengerRoutesPath = passengerRoutesPath;
        this.passengerCardsPath = passengerCardsPath;
        this.passengerTicketsPath = passengerTicketsPath;
        this.passengerTripsPath = passengerTripsPath;
        this.farePricesPath = farePricesPath;
        this.fareDiscountsPath = fareDiscountsPath;
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
    public List<ExternalCardHistoryResponse> getCards(String userId) {
        if (mockEnabled) {
            Instant now = Instant.now();
            return List.of(new ExternalCardHistoryResponse(
                    UUID.randomUUID(), "UID-" + shortToken().toUpperCase(), "ACTIVE", "STANDARD", now, now));
        }
        return Arrays.asList(get(
                passengerCardsPath, "userId", userId, ExternalCardHistoryResponse[].class, "passenger cards"));
    }

    @Override
    public List<ExternalTicketHistoryResponse> getTickets(String userId) {
        if (mockEnabled) {
            UUID ticketId = UUID.randomUUID();
            LocalDate today = LocalDate.now();
            return List.of(new ExternalTicketHistoryResponse(
                    ticketId, "SINGLE_TRIP", "METRO", null, "ACTIVE", new BigDecimal("15000"),
                    null, null, today, today.plusDays(1), ticketId.toString(), false, Instant.now()));
        }
        return Arrays.asList(get(
                passengerTicketsPath, "userId", userId, ExternalTicketHistoryResponse[].class, "passenger tickets"));
    }

    @Override
    public List<ExternalTravelHistoryResponse> getTrips(String userId) {
        if (mockEnabled) {
            Instant now = Instant.now();
            return List.of(new ExternalPassengerTripResponse(
                    UUID.randomUUID(), UUID.randomUUID(), "METRO", "CL", "HD", now.minusSeconds(1800), now,
                    new BigDecimal("8.5"), new BigDecimal("15000")));
        }
        return Arrays.asList(get(
                passengerTripsPath, "userId", userId, ExternalTravelHistoryResponse[].class, "passenger trips"));
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

    @Override
    public List<ExternalFarePriceResponse> getStations() {
        if (mockEnabled) {
            return mockFarePrices();
        }
        return Arrays.asList(get(
                passengerStationsPath, ExternalFarePriceResponse[].class, "passenger stations"));
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

    private <T> T post(String path, Object body, Class<T> responseType, String operation) {
        T response = restClient.post().uri(path).contentType(MediaType.APPLICATION_JSON)
                .body(body).retrieve().body(responseType);
        if (response == null) {
            throw new IllegalStateException("Level 5 returned an empty response for " + operation);
        }
        return response;
    }

    private <T> T get(String path, String variableName, String variableValue, Class<T> responseType, String operation) {
        T response = restClient.get().uri(path, Map.of(variableName, variableValue)).retrieve().body(responseType);
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

    private ExternalTicketHistoryResponse toTicketHistory(ExternalPassengerTicketResponse passengerTicket) {
        ExternalTicketHistoryResponse history = new ExternalTicketHistoryResponse();
        history.setTicketId(passengerTicket.ticketId() == null ? null : passengerTicket.ticketId().toString());
        history.setType(passengerTicket.type());
        history.setStatus(passengerTicket.status());
        history.setPrice(passengerTicket.price());
        history.setValidFrom(passengerTicket.validFrom());
        history.setValidTo(passengerTicket.validTo());
        history.setPurchasedAt(toLocalDateTime(passengerTicket.purchasedAt()));
        return history;
    }

    private ExternalCardHistoryResponse toCardHistory(ExternalPassengerCardResponse passengerCard) {
        ExternalCardHistoryResponse history = new ExternalCardHistoryResponse();
        history.setCardId(passengerCard.cardId() == null ? null : passengerCard.cardId().toString());
        history.setCardUid(passengerCard.cardUid());
        history.setStatus(passengerCard.status());
        history.setType(passengerCard.type());
        history.setActivatedAt(toLocalDateTime(passengerCard.activatedAt()));
        history.setLinkedAt(toLocalDateTime(passengerCard.linkedAt()));
        return history;
    }

    private ExternalTravelHistoryResponse toTravelHistory(String accountId, ExternalPassengerTripResponse passengerTrip) {
        ExternalTravelHistoryResponse history = new ExternalTravelHistoryResponse();
        history.setExternalTripId(passengerTrip.tripId() == null ? null : passengerTrip.tripId().toString());
        history.setPassengerAccountId(accountId);
        history.setTicketExternalId(passengerTrip.ticketId() == null ? null : passengerTrip.ticketId().toString());
        history.setCheckinStationCode(passengerTrip.tapInStationCode());
        history.setCheckoutStationCode(passengerTrip.tapOutStationCode());
        history.setCheckinTime(toLocalDateTime(passengerTrip.tapInAt()));
        history.setCheckoutTime(toLocalDateTime(passengerTrip.tapOutAt()));
        history.setTransportType(passengerTrip.mode());
        return history;
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
