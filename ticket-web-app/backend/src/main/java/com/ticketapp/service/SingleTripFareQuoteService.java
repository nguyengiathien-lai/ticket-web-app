package com.ticketapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketapp.client.level5.Level5Client;
import com.ticketapp.dto.external.ExternalFarePriceResponse;
import com.ticketapp.dto.external.ExternalPassengerRouteResponse;
import com.ticketapp.dto.external.ExternalPassengerStationResponse;
import com.ticketapp.dto.fare.SingleTripFareQuoteResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SingleTripFareQuoteService {

    private static final String QUOTE_KEY_PREFIX = "passenger:fare:single-trip:";
    private static final String CURRENCY = "VND";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Level5Client level5Client;
    private final Duration quoteTtl;

    public SingleTripFareQuoteService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            Level5Client level5Client,
            @Value("${app.cache.catalog-data-ttl-seconds:86400}") int catalogDataCacheTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.level5Client = level5Client;
        this.quoteTtl = Duration.ofSeconds(catalogDataCacheTtlSeconds);
    }

    public SingleTripFareQuoteResponse getQuote(String mode, String fromStationId, String toStationId) {
        String normalizedMode = requireText(mode, "mode").toUpperCase(Locale.ROOT);
        String normalizedFrom = requireText(fromStationId, "fromStationId");
        String normalizedTo = requireText(toStationId, "toStationId");
        String key = quoteKey(normalizedMode, normalizedFrom, normalizedTo);

        Optional<SingleTripFareQuoteResponse> cachedQuote = readQuote(key);
        if (cachedQuote.isPresent()) {
            return cachedQuote.get();
        }

        SingleTripFareQuoteResponse quote = calculateQuote(
                normalizedMode,
                normalizedFrom,
                normalizedTo,
                level5Client.getFarePrices(),
                level5Client.getStations());
        cacheQuote(key, quote);
        cacheQuote(quoteKey(normalizedMode, normalizedTo, normalizedFrom), reverseQuote(quote));
        return quote;
    }

    public void refreshQuotes(List<ExternalFarePriceResponse> farePrices) {
        List<ExternalPassengerStationResponse> stations = level5Client.getStations();
        List<ExternalPassengerRouteResponse> routes = level5Client.getRoutes();

        for (ExternalFarePriceResponse farePrice : farePrices) {
            if (farePrice == null || farePrice.singleTrip() == null || farePrice.mode() == null) {
                continue;
            }

            String mode = farePrice.mode().trim().toUpperCase(Locale.ROOT);
            Set<String> routeIdsForMode = routes.stream()
                    .filter(route -> sameMode(route.type(), mode))
                    .filter(route -> route.id() != null)
                    .map(route -> route.id().toString())
                    .collect(Collectors.toSet());
            List<ExternalPassengerStationResponse> stationsForMode = stations.stream()
                    .filter(station -> station.routeId() != null)
                    .filter(station -> routeIdsForMode.isEmpty() || routeIdsForMode.contains(station.routeId().toString()))
                    .toList();

            for (int fromIndex = 0; fromIndex < stationsForMode.size(); fromIndex += 1) {
                for (int toIndex = 0; toIndex < stationsForMode.size(); toIndex += 1) {
                    if (fromIndex == toIndex) {
                        continue;
                    }

                    ExternalPassengerStationResponse from = stationsForMode.get(fromIndex);
                    ExternalPassengerStationResponse to = stationsForMode.get(toIndex);
                    if (!from.routeId().equals(to.routeId())) {
                        continue;
                    }

                    SingleTripFareQuoteResponse quote = calculateQuote(mode, from, to, farePrice.singleTrip());
                    cacheQuote(quoteKey(mode, from.id().toString(), to.id().toString()), quote);
                }
            }
        }
    }

    private SingleTripFareQuoteResponse calculateQuote(
            String mode,
            String fromStationId,
            String toStationId,
            List<ExternalFarePriceResponse> farePrices,
            List<ExternalPassengerStationResponse> stations) {
        ExternalFarePriceResponse.SingleTripPrice singleTrip = farePrices.stream()
                .filter(farePrice -> sameMode(farePrice.mode(), mode))
                .map(ExternalFarePriceResponse::singleTrip)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Single-trip fare rule not found for mode: " + mode));
        ExternalPassengerStationResponse from = findStation(stations, fromStationId, "fromStationId");
        ExternalPassengerStationResponse to = findStation(stations, toStationId, "toStationId");
        return calculateQuote(mode, from, to, singleTrip);
    }

    private SingleTripFareQuoteResponse calculateQuote(
            String mode,
            ExternalPassengerStationResponse from,
            ExternalPassengerStationResponse to,
            ExternalFarePriceResponse.SingleTripPrice singleTrip) {
        if (from.kmMarker() == null || to.kmMarker() == null) {
            throw new IllegalArgumentException("Both stations must include kmMarker to quote a single-trip fare");
        }

        BigDecimal distanceKm = to.kmMarker().subtract(from.kmMarker()).abs();
        BigDecimal baseFare = firstValue(singleTrip.baseFare(), BigDecimal.ZERO);
        BigDecimal ratePerKm = firstValue(singleTrip.ratePerKm(), BigDecimal.ZERO);
        BigDecimal rawPrice = baseFare.add(distanceKm.multiply(ratePerKm));
        BigDecimal price = clamp(rawPrice, singleTrip.minPrice(), singleTrip.maxPrice())
                .setScale(0, RoundingMode.HALF_UP);

        return SingleTripFareQuoteResponse.builder()
                .mode(mode)
                .fromStationId(from.id().toString())
                .toStationId(to.id().toString())
                .distanceKm(distanceKm.setScale(3, RoundingMode.HALF_UP))
                .baseFare(baseFare)
                .ratePerKm(ratePerKm)
                .minPrice(singleTrip.minPrice())
                .maxPrice(singleTrip.maxPrice())
                .price(price)
                .currency(CURRENCY)
                .build();
    }

    private Optional<SingleTripFareQuoteResponse> readQuote(String key) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(json, SingleTripFareQuoteResponse.class));
        } catch (JsonProcessingException exception) {
            return Optional.empty();
        }
    }

    private void cacheQuote(String key, SingleTripFareQuoteResponse quote) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(quote), quoteTtl);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to write single-trip fare quote cache entry: " + key, exception);
        }
    }

    private SingleTripFareQuoteResponse reverseQuote(SingleTripFareQuoteResponse quote) {
        return SingleTripFareQuoteResponse.builder()
                .mode(quote.getMode())
                .fromStationId(quote.getToStationId())
                .toStationId(quote.getFromStationId())
                .distanceKm(quote.getDistanceKm())
                .baseFare(quote.getBaseFare())
                .ratePerKm(quote.getRatePerKm())
                .minPrice(quote.getMinPrice())
                .maxPrice(quote.getMaxPrice())
                .price(quote.getPrice())
                .currency(quote.getCurrency())
                .build();
    }

    private ExternalPassengerStationResponse findStation(
            List<ExternalPassengerStationResponse> stations,
            String stationId,
            String fieldName) {
        return stations.stream()
                .filter(station -> station.id().toString().equals(stationId)
                        || (station.code() != null && station.code().equalsIgnoreCase(stationId)))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown " + fieldName + ": " + stationId));
    }

    private BigDecimal clamp(BigDecimal value, BigDecimal minPrice, BigDecimal maxPrice) {
        BigDecimal result = value;
        if (minPrice != null && result.compareTo(minPrice) < 0) {
            result = minPrice;
        }
        if (maxPrice != null && result.compareTo(maxPrice) > 0) {
            result = maxPrice;
        }
        return result;
    }

    private boolean sameMode(String value, String mode) {
        return value != null && value.trim().equalsIgnoreCase(mode);
    }

    private String quoteKey(String mode, String fromStationId, String toStationId) {
        return QUOTE_KEY_PREFIX + mode + ":" + fromStationId + ":" + toStationId;
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
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
