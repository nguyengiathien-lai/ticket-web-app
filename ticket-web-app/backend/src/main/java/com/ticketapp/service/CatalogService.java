package com.ticketapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketapp.client.level5.Level5Client;
import com.ticketapp.dto.catalog.CardTypeSyncRequest;
import com.ticketapp.dto.catalog.TicketTypeSyncRequest;
import com.ticketapp.dto.external.ExternalCardTypeResponse;
import com.ticketapp.dto.external.ExternalTicketTypeResponse;
import com.ticketapp.dto.purchase.CardTypeResponse;
import com.ticketapp.dto.ticket.TicketTypeResponse;
import com.ticketapp.entity.CardType;
import com.ticketapp.entity.TicketType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class CatalogService {

    private static final String CARD_TYPE_CODES_KEY = "catalog:card-types:codes";
    private static final String TICKET_TYPE_CODES_KEY = "catalog:ticket-types:codes";
    private static final String CARD_TYPE_KEY_PREFIX = "catalog:card-types:";
    private static final String TICKET_TYPE_KEY_PREFIX = "catalog:ticket-types:";
    private static final String CARD_TYPES_LOADED_KEY = "catalog:card-types:loaded";
    private static final String TICKET_TYPES_LOADED_KEY = "catalog:ticket-types:loaded";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Level5Client level5Client;
    private final Duration catalogRefreshTtl;

    public CatalogService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            Level5Client level5Client,
            @Value("${app.cache.catalog-refresh-ttl-seconds:600}") long catalogRefreshTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.level5Client = level5Client;
        this.catalogRefreshTtl = Duration.ofSeconds(Math.max(catalogRefreshTtlSeconds, 1));
    }

    public List<CardTypeResponse> getActiveCardTypes() {
        ensureCardTypesLoaded();
        return getCardTypesFromRedis()
                .stream()
                .filter(cardType -> Boolean.TRUE.equals(cardType.getActive()))
                .sorted(Comparator.comparing(CardType::getCode))
                .map(CardTypeResponse::from)
                .toList();
    }

    public List<TicketTypeResponse> getActiveTicketTypes() {
        ensureTicketTypesLoaded();
        return getTicketTypesFromRedis()
                .stream()
                .filter(ticketType -> Boolean.TRUE.equals(ticketType.getActive()))
                .sorted(Comparator.comparing(TicketType::getCode))
                .map(TicketTypeResponse::from)
                .toList();
    }

    public CardType requireActiveCardType(String code) {
        ensureCardTypesLoaded();
        CardType cardType = getCardTypeFromRedis(code)
                .orElseThrow(() -> new IllegalArgumentException("Card type not found in Redis cache: " + code));

        if (!Boolean.TRUE.equals(cardType.getActive())) {
            throw new IllegalArgumentException("Card type is inactive: " + code);
        }

        return cardType;
    }

    public TicketType requireActiveTicketType(String code) {
        ensureTicketTypesLoaded();
        TicketType ticketType = getTicketTypeFromRedis(code)
                .orElseThrow(() -> new IllegalArgumentException("Ticket type not found in Redis cache: " + code));

        if (!Boolean.TRUE.equals(ticketType.getActive())) {
            throw new IllegalArgumentException("Ticket type is inactive: " + code);
        }

        return ticketType;
    }

    public List<CardTypeResponse> cacheCardTypes(List<CardTypeSyncRequest> requests) {
        List<CardType> cached = requests.stream()
                .map(this::cacheCardType)
                .toList();
        markCatalogLoaded(CARD_TYPES_LOADED_KEY, requests.stream()
                .map(CardTypeSyncRequest::getExpiresAt)
                .toList());
        return cached.stream()
                .map(CardTypeResponse::from)
                .toList();
    }

    public List<TicketTypeResponse> cacheTicketTypes(List<TicketTypeSyncRequest> requests) {
        List<TicketType> cached = requests.stream()
                .map(this::cacheTicketType)
                .toList();
        markCatalogLoaded(TICKET_TYPES_LOADED_KEY, requests.stream()
                .map(TicketTypeSyncRequest::getExpiresAt)
                .toList());
        return cached.stream()
                .map(TicketTypeResponse::from)
                .toList();
    }

    private void ensureCardTypesLoaded() {
        if (catalogLoaded(CARD_TYPES_LOADED_KEY)) {
            return;
        }
        redisTemplate.delete(CARD_TYPE_CODES_KEY);
        cacheCardTypes(level5Client.getCardTypes().stream().map(this::toSyncRequest).toList());
    }

    private void ensureTicketTypesLoaded() {
        if (catalogLoaded(TICKET_TYPES_LOADED_KEY)) {
            return;
        }
        redisTemplate.delete(TICKET_TYPE_CODES_KEY);
        cacheTicketTypes(level5Client.getTicketTypes().stream().map(this::toSyncRequest).toList());
    }

    private CardTypeSyncRequest toSyncRequest(ExternalCardTypeResponse external) {
        CardTypeSyncRequest request = new CardTypeSyncRequest();
        request.setExternalCardTypeId(external.getExternalCardTypeId());
        request.setCode(external.getCode());
        request.setName(external.getName());
        request.setDurationDays(external.getDurationDays());
        request.setPrice(external.getPrice());
        request.setCurrency(external.getCurrency());
        request.setDescription(external.getDescription());
        request.setActive(external.getActive());
        request.setExpiresAt(external.getExpiresAt());
        return request;
    }

    private TicketTypeSyncRequest toSyncRequest(ExternalTicketTypeResponse external) {
        TicketTypeSyncRequest request = new TicketTypeSyncRequest();
        request.setExternalTicketTypeId(external.getExternalTicketTypeId());
        request.setCode(external.getCode());
        request.setName(external.getName());
        request.setDurationDays(external.getDurationDays());
        request.setPrice(external.getPrice());
        request.setCurrency(external.getCurrency());
        request.setDescription(external.getDescription());
        request.setActive(external.getActive());
        request.setExpiresAt(external.getExpiresAt());
        return request;
    }

    private CardType cacheCardType(CardTypeSyncRequest request) {
        CardType cardType = new CardType();
        cardType.setExternalCardTypeId(request.getExternalCardTypeId());
        cardType.setCode(request.getCode());
        cardType.setName(request.getName());
        cardType.setDurationDays(request.getDurationDays());
        cardType.setPrice(request.getPrice());
        cardType.setCurrency(request.getCurrency());
        cardType.setDescription(request.getDescription());
        cardType.setActive(request.getActive() == null || request.getActive());
        cardType.setCachedAt(LocalDateTime.now());
        cardType.setExpiresAt(request.getExpiresAt());

        writeJson(cardTypeKey(cardType.getCode()), cardType, cardType.getExpiresAt());
        redisTemplate.opsForSet().add(CARD_TYPE_CODES_KEY, cardType.getCode());
        return cardType;
    }

    private TicketType cacheTicketType(TicketTypeSyncRequest request) {
        TicketType ticketType = new TicketType();
        ticketType.setExternalTicketTypeId(request.getExternalTicketTypeId());
        ticketType.setCode(request.getCode());
        ticketType.setName(request.getName());
        ticketType.setDurationDays(request.getDurationDays());
        ticketType.setPrice(request.getPrice());
        ticketType.setCurrency(request.getCurrency());
        ticketType.setDescription(request.getDescription());
        ticketType.setActive(request.getActive() == null || request.getActive());
        ticketType.setCachedAt(LocalDateTime.now());
        ticketType.setExpiresAt(request.getExpiresAt());

        writeJson(ticketTypeKey(ticketType.getCode()), ticketType, ticketType.getExpiresAt());
        redisTemplate.opsForSet().add(TICKET_TYPE_CODES_KEY, ticketType.getCode());
        return ticketType;
    }

    private List<CardType> getCardTypesFromRedis() {
        Set<String> codes = redisTemplate.opsForSet().members(CARD_TYPE_CODES_KEY);
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }

        return codes.stream()
                .map(this::getCardTypeFromRedis)
                .flatMap(Optional::stream)
                .toList();
    }

    private List<TicketType> getTicketTypesFromRedis() {
        Set<String> codes = redisTemplate.opsForSet().members(TICKET_TYPE_CODES_KEY);
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }

        return codes.stream()
                .map(this::getTicketTypeFromRedis)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<CardType> getCardTypeFromRedis(String code) {
        return readJson(cardTypeKey(code), CardType.class);
    }

    private Optional<TicketType> getTicketTypeFromRedis(String code) {
        return readJson(ticketTypeKey(code), TicketType.class);
    }

    private <T> Optional<T> readJson(String key, Class<T> valueType) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(json, valueType));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to read Redis catalog cache entry: " + key, exception);
        }
    }

    private void writeJson(String key, Object value, LocalDateTime expiresAt) {
        try {
            String json = objectMapper.writeValueAsString(value);
            Duration ttl = ttlUntil(expiresAt);
            if (ttl == null) {
                redisTemplate.opsForValue().set(key, json);
            } else {
                redisTemplate.opsForValue().set(key, json, ttl);
            }
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to write Redis catalog cache entry: " + key, exception);
        }
    }

    private Duration ttlUntil(LocalDateTime expiresAt) {
        if (expiresAt == null) {
            return null;
        }

        long expiresAtEpoch = expiresAt.atZone(ZoneId.systemDefault()).toEpochSecond();
        long nowEpoch = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond();
        long seconds = expiresAtEpoch - nowEpoch;
        return Duration.ofSeconds(Math.max(seconds, 1));
    }

    private boolean catalogLoaded(String key) {
        return "true".equals(redisTemplate.opsForValue().get(key));
    }

    private void markCatalogLoaded(String key, List<LocalDateTime> expirations) {
        Duration markerTtl = expirations.stream()
                .filter(Objects::nonNull)
                .map(this::ttlUntil)
                .filter(ttl -> !ttl.isNegative() && !ttl.isZero())
                .min(Duration::compareTo)
                .map(ttl -> ttl.compareTo(catalogRefreshTtl) < 0 ? ttl : catalogRefreshTtl)
                .orElse(catalogRefreshTtl);
        redisTemplate.opsForValue().set(key, "true", markerTtl);
    }

    private String cardTypeKey(String code) {
        return CARD_TYPE_KEY_PREFIX + Objects.requireNonNull(code);
    }

    private String ticketTypeKey(String code) {
        return TICKET_TYPE_KEY_PREFIX + Objects.requireNonNull(code);
    }
}
