package com.ticketapp.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketapp.client.level5.Level5Client;
import com.ticketapp.dto.ApiResponse;
import com.ticketapp.dto.card.CardResponse;
import com.ticketapp.dto.external.ExternalDiscountResponse;
import com.ticketapp.dto.external.ExternalFarePriceResponse;
import com.ticketapp.dto.external.ExternalPassengerRouteResponse;
import com.ticketapp.dto.external.ExternalPassengerStationResponse;
import com.ticketapp.dto.external.ExternalTravelHistoryResponse;
import com.ticketapp.dto.ticket.TicketResponse;
import com.ticketapp.service.CardService;
import com.ticketapp.service.TicketService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

@RestController
public class PassengerController {

    private static final Logger log = LoggerFactory.getLogger(PassengerController.class);

    private final Level5Client level5Client;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration passengerCacheTtl;
    private final CardService cardService;
    private final TicketService ticketService;

    public PassengerController(
            Level5Client level5Client,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.cache.passenger-data-ttl-seconds:86400}") int passengerCacheTtlSeconds,
            CardService cardService,
            TicketService ticketService) {
        this.level5Client = level5Client;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.passengerCacheTtl = Duration.ofSeconds(passengerCacheTtlSeconds);
        this.cardService = cardService;
        this.ticketService = ticketService;
    }

    @GetMapping("/passengers/stations")
    public ResponseEntity<ApiResponse<List<ExternalPassengerStationResponse>>> getStations() {
        return ResponseEntity.ok(ApiResponse.success(
                cachedList(
                        "passenger:stations",
                        ExternalPassengerStationResponse.class,
                        level5Client::getStations),
                "Passenger stations retrieved successfully"));
    }

    @GetMapping("/passengers/routes")
    public ResponseEntity<ApiResponse<List<ExternalPassengerRouteResponse>>> getRoutes() {
        return ResponseEntity.ok(ApiResponse.success(
                cachedList(
                        "passenger:routes",
                        ExternalPassengerRouteResponse.class,
                        level5Client::getRoutes),
                "Passenger routes retrieved successfully"));
    }

    @GetMapping({"/passengers/{userId}/cards", "/passenger/{userId}/cards"})
    public ResponseEntity<ApiResponse<List<CardResponse>>> getPassengerCards(
            @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                cardService.getCardsForPassenger(normalizeUserId(userId)),
                "Passenger cards retrieved successfully"));
    }

    @GetMapping({"/passengers/{userId}/tickets", "/passenger/{userId}/tickets"})
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getPassengerTickets(
            @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                ticketService.getTicketsForPassenger(normalizeUserId(userId)),
                "Passenger tickets retrieved successfully"));
    }

    @GetMapping({"/passengers/{userId}/trips", "/passenger/{userId}/trips"})
    public ResponseEntity<ApiResponse<List<ExternalTravelHistoryResponse>>> getPassengerTrips(
            @PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success(
                cachedList(
                        "passenger:" + normalizeUserId(userId) + ":trips",
                        ExternalTravelHistoryResponse.class,
                        () -> level5Client.getTravelHistory(userId)),
                "Passenger trips retrieved successfully"));
    }

    @GetMapping("/passengers/fare/prices")
    public ResponseEntity<ApiResponse<List<ExternalFarePriceResponse>>> getFarePrices() {
        return ResponseEntity.ok(ApiResponse.success(
                cachedList(
                        "passenger:fare:prices",
                        ExternalFarePriceResponse.class,
                        level5Client::getFarePrices),
                "Fare prices retrieved successfully"));
    }

    @GetMapping("/passengers/fare/discounts")
    public ResponseEntity<ApiResponse<List<ExternalDiscountResponse>>> getFareDiscounts() {
        return ResponseEntity.ok(ApiResponse.success(
                cachedList(
                        "passenger:fare:discounts",
                        ExternalDiscountResponse.class,
                        level5Client::getFareDiscounts),
                "Fare discounts retrieved successfully"));
    }

    private <T> List<T> cachedList(String cacheKey, Class<T> elementType, Supplier<List<T>> loader) {
        try {
            List<T> freshData = loader.get();
            log.info("Loaded fresh passenger catalog data; cacheKey={}, size={}", cacheKey, freshData.size());
            cacheList(cacheKey, freshData);
            return freshData;
        } catch (RuntimeException exception) {
            log.warn("Could not load fresh passenger catalog data; attempting Redis fallback. cacheKey={}, cause={}: {}",
                    cacheKey,
                    exception.getClass().getSimpleName(),
                    exception.getMessage());
            return readCachedList(cacheKey, elementType, exception);
        }
    }

    private void cacheList(String cacheKey, List<?> data) {
        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(data), passengerCacheTtl);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to write passenger cache entry: " + cacheKey, exception);
        }
    }

    private <T> List<T> readCachedList(String cacheKey, Class<T> elementType, RuntimeException originalException) {
        String json = redisTemplate.opsForValue().get(cacheKey);
        if (json == null || json.isBlank()) {
            log.warn("Redis fallback cache miss; cacheKey={}", cacheKey);
            throw originalException;
        }

        try {
            List<T> cachedData = objectMapper.readValue(
                    json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, elementType));
            log.warn("Returning Redis fallback passenger catalog data; cacheKey={}, size={}", cacheKey, cachedData.size());
            return cachedData;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to read passenger cache entry: " + cacheKey, exception);
        }
    }

    private String normalizeUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Passenger user ID is required");
        }
        return userId.trim();
    }
}
