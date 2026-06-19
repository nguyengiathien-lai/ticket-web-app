package com.ticketapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketapp.dto.catalog.CardTypeSyncRequest;
import com.ticketapp.dto.catalog.TicketTypeSyncRequest;
import com.ticketapp.dto.purchase.CardTypeResponse;
import com.ticketapp.dto.ticket.TicketTypeResponse;
import com.ticketapp.entity.CardType;
import com.ticketapp.entity.TicketType;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public CatalogService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public List<CardTypeResponse> getActiveCardTypes() {
        return getCardTypesFromRedis()
                .stream()
                .filter(cardType -> Boolean.TRUE.equals(cardType.getActive()))
                .sorted(Comparator.comparing(CardType::getCode))
                .map(CardTypeResponse::from)
                .toList();
    }

    public List<TicketTypeResponse> getActiveTicketTypes() {
        return getTicketTypesFromRedis()
                .stream()
                .filter(ticketType -> Boolean.TRUE.equals(ticketType.getActive()))
                .sorted(Comparator.comparing(TicketType::getCode))
                .map(TicketTypeResponse::from)
                .toList();
    }

    public CardType requireActiveCardType(String code) {
        CardType cardType = getCardTypeFromRedis(code)
                .orElseThrow(() -> new IllegalArgumentException("Card type not found in Redis cache: " + code));

        if (!Boolean.TRUE.equals(cardType.getActive())) {
            throw new IllegalArgumentException("Card type is inactive: " + code);
        }

        return cardType;
    }

    public TicketType requireActiveTicketType(String code) {
        TicketType ticketType = getTicketTypeFromRedis(code)
                .orElseThrow(() -> new IllegalArgumentException("Ticket type not found in Redis cache: " + code));

        if (!Boolean.TRUE.equals(ticketType.getActive())) {
            throw new IllegalArgumentException("Ticket type is inactive: " + code);
        }

        return ticketType;
    }

    public List<CardTypeResponse> cacheCardTypes(List<CardTypeSyncRequest> requests) {
        return requests.stream()
                .map(this::cacheCardType)
                .map(CardTypeResponse::from)
                .toList();
    }

    public List<TicketTypeResponse> cacheTicketTypes(List<TicketTypeSyncRequest> requests) {
        return requests.stream()
                .map(this::cacheTicketType)
                .map(TicketTypeResponse::from)
                .toList();
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

    private String cardTypeKey(String code) {
        return CARD_TYPE_KEY_PREFIX + Objects.requireNonNull(code);
    }

    private String ticketTypeKey(String code) {
        return TICKET_TYPE_KEY_PREFIX + Objects.requireNonNull(code);
    }
}
