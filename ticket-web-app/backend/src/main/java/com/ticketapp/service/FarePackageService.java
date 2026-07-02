package com.ticketapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketapp.client.level5.Level5Client;
import com.ticketapp.dto.external.ExternalFarePriceResponse;
import com.ticketapp.dto.fare.FarePackageResponse;
import com.ticketapp.entity.FarePackage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class FarePackageService {

    private static final String FARE_PACKAGE_KEY_PREFIX = "catalog:fare-packages:";
    private static final String FARE_PACKAGES_KEY = "catalog:fare-packages:codes";
    private static final String FARE_PACKAGES_LOADED_KEY = "catalog:fare-packages:loaded";
    private static final String CURRENCY = "VND";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Level5Client level5Client;
    private final Duration catalogRefreshTtl;

    public FarePackageService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            Level5Client level5Client,
            @Value("${app.cache.catalog-refresh-ttl-seconds:600}") int catalogRefreshTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.level5Client = level5Client;
        this.catalogRefreshTtl = Duration.ofSeconds(catalogRefreshTtlSeconds);
    }

    public List<FarePackageResponse> getActiveFarePackages() {
        refreshFarePackagesIfEmpty();
        Set<String> codes = redisTemplate.opsForSet().members(FARE_PACKAGES_KEY);
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }

        return codes.stream()
                .map(code -> readFarePackage(farePackageKey(code)))
                .flatMap(Optional::stream)
                .filter(farePackage -> Boolean.TRUE.equals(farePackage.getActive()))
                .sorted(Comparator.comparing(
                        FarePackage::getCode,
                        Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(FarePackageResponse::from)
                .toList();
    }

    public FarePackage requireActiveFarePackage(String code) {
        String normalizedCode = requireText(code, "fare package code");
        refreshFarePackagesIfEmpty();
        return readFarePackage(farePackageKey(normalizedCode))
                .filter(farePackage -> Boolean.TRUE.equals(farePackage.getActive()))
                .orElseThrow(() -> new IllegalArgumentException("Fare package not found or inactive"));
    }

    private void refreshFarePackagesIfEmpty() {
        if ("true".equals(redisTemplate.opsForValue().get(FARE_PACKAGES_LOADED_KEY))) {
            return;
        }
        level5Client.getFarePrices().stream()
                .flatMap(farePrice -> toFarePackages(farePrice).stream())
                .forEach(this::cacheFarePackage);
        redisTemplate.opsForValue().set(FARE_PACKAGES_LOADED_KEY, "true", catalogRefreshTtl);
    }

    private List<FarePackage> toFarePackages(ExternalFarePriceResponse farePrice) {
        if (farePrice == null || farePrice.mode() == null || farePrice.mode().isBlank()) {
            return List.of();
        }

        String mode = farePrice.mode().trim().toUpperCase(Locale.ROOT);
        List<FarePackage> passPackages = farePrice.passPrices() == null
                ? List.of()
                : farePrice.passPrices().stream()
                        .map(passPrice -> toPassPackage(mode, passPrice))
                        .toList();

        FarePackage singleTrip = toSingleTripPackage(mode, farePrice.singleTrip());
        if (singleTrip == null) {
            return passPackages;
        }
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(singleTrip), passPackages.stream()).toList();
    }

    private FarePackage toSingleTripPackage(String mode, ExternalFarePriceResponse.SingleTripPrice singleTrip) {
        if (singleTrip == null) {
            return null;
        }
        FarePackage farePackage = basePackage(mode);
        farePackage.setCode(mode + "_SINGLE_TRIP");
        farePackage.setName(mode + " single trip");
        farePackage.setKind("SINGLE_TRIP");
        farePackage.setDurationType("SINGLE_TRIP");
        farePackage.setDurationDays(1);
        farePackage.setPrice(firstValue(singleTrip.baseFare(), singleTrip.minPrice(), BigDecimal.ZERO));
        farePackage.setDescription("Single trip fare for " + mode);
        return farePackage;
    }

    private FarePackage toPassPackage(String mode, ExternalFarePriceResponse.PassPriceItem passPrice) {
        FarePackage farePackage = basePackage(mode);
        String durationType = requireText(passPrice.durationType(), "fare package duration type")
                .toUpperCase(Locale.ROOT);
        String scope = passPrice.scope() == null || passPrice.scope().isBlank()
                ? null
                : passPrice.scope().trim().toUpperCase(Locale.ROOT);
        farePackage.setKind("PASS");
        farePackage.setDurationType(durationType);
        farePackage.setDurationMonths(passPrice.durationMonths());
        farePackage.setDurationDays(durationDays(durationType, passPrice.durationMonths()));
        farePackage.setScope(scope);
        farePackage.setCode(packageCode(mode, durationType, scope));
        farePackage.setName(packageName(mode, durationType, scope));
        farePackage.setPrice(passPrice.price());
        farePackage.setDescription(farePackage.getName());
        return farePackage;
    }

    private FarePackage basePackage(String mode) {
        FarePackage farePackage = new FarePackage();
        farePackage.setMode(mode);
        farePackage.setCurrency(CURRENCY);
        farePackage.setActive(true);
        farePackage.setCachedAt(LocalDateTime.now());
        return farePackage;
    }

    private String packageCode(String mode, String durationType, String scope) {
        return scope == null ? mode + "_" + durationType : mode + "_" + durationType + "_" + scope;
    }

    private String packageName(String mode, String durationType, String scope) {
        return scope == null
                ? mode + " " + durationType.toLowerCase(Locale.ROOT) + " pass"
                : mode + " " + durationType.toLowerCase(Locale.ROOT) + " " + scope.toLowerCase(Locale.ROOT) + " pass";
    }

    private Integer durationDays(String durationType, Integer durationMonths) {
        return switch (durationType) {
            case "DAILY" -> 1;
            case "WEEKLY" -> 7;
            case "MONTHLY" -> durationMonths == null ? 30 : durationMonths * 30;
            default -> null;
        };
    }

    private void cacheFarePackage(FarePackage farePackage) {
        String key = farePackageKey(farePackage.getCode());
        String json = writeJson(farePackage, key);
        Duration ttl = ttlUntil(farePackage.getExpiresAt());
        if (ttl == null) {
            redisTemplate.opsForValue().set(key, json);
        } else {
            redisTemplate.opsForValue().set(key, json, ttl);
        }
        redisTemplate.opsForSet().add(FARE_PACKAGES_KEY, farePackage.getCode());
    }

    private Optional<FarePackage> readFarePackage(String key) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, FarePackage.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to read Redis fare package cache entry: " + key, exception);
        }
    }

    private String writeJson(FarePackage farePackage, String key) {
        try {
            return objectMapper.writeValueAsString(farePackage);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to write Redis fare package cache entry: " + key, exception);
        }
    }

    private Duration ttlUntil(LocalDateTime expiresAt) {
        if (expiresAt == null) {
            return null;
        }
        long expiresAtEpoch = expiresAt.atZone(ZoneId.systemDefault()).toEpochSecond();
        long nowEpoch = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond();
        return Duration.ofSeconds(Math.max(expiresAtEpoch - nowEpoch, 1));
    }

    private String farePackageKey(String code) {
        return FARE_PACKAGE_KEY_PREFIX + code.trim();
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing " + fieldName + " in fare package data");
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
