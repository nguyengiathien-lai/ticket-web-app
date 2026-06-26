package com.ticketapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketapp.client.level5.Level5Client;
import com.ticketapp.dto.card.CardResponse;
import com.ticketapp.dto.catalog.CardTypeSyncRequest;
import com.ticketapp.dto.external.ExternalCardHistoryResponse;
import com.ticketapp.dto.external.ExternalCardTypeResponse;
import com.ticketapp.dto.purchase.CardTypeResponse;
import com.ticketapp.entity.CardType;
import com.ticketapp.entity.PhysicalCard;
import org.springframework.beans.factory.annotation.Value;
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
public class CardService {

    private static final String CARD_KEY_PREFIX = "cache:cards:id:";
    private static final String CARD_UID_KEY_PREFIX = "cache:cards:uid:";
    private static final String PASSENGER_CARDS_KEY_PREFIX = "cache:cards:passenger:";
    private static final String PASSENGER_CARDS_LOADED_KEY_PREFIX = "cache:cards:loaded:";
    private static final Duration HISTORY_LOADED_TTL = Duration.ofMinutes(10);
    private static final String CARD_TYPE_CODES_KEY = "catalog:card-types:codes";
    private static final String CARD_TYPE_KEY_PREFIX = "catalog:card-types:";
    private static final String CARD_TYPES_LOADED_KEY = "catalog:card-types:loaded";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Level5Client level5Client;
    private final Duration catalogRefreshTtl;

    public CardService(
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

    public CardType requireActiveCardType(String code) {
        ensureCardTypesLoaded();
        CardType cardType = getCardTypeFromRedis(code)
                .orElseThrow(() -> new IllegalArgumentException("Card type not found in Redis cache: " + code));

        if (!Boolean.TRUE.equals(cardType.getActive())) {
            throw new IllegalArgumentException("Card type is inactive: " + code);
        }

        return cardType;
    }

    public List<CardTypeResponse> cacheCardTypes(List<CardTypeSyncRequest> requests) {
        List<CardType> cached = requests.stream()
                .map(this::cacheCardType)
                .toList();
        markCatalogLoaded(requests.stream()
                .map(CardTypeSyncRequest::getExpiresAt)
                .toList());
        return cached.stream()
                .map(CardTypeResponse::from)
                .toList();
    }

    public PhysicalCard cacheCard(PhysicalCard card) {
        validate(card);
        card.setCachedAt(LocalDateTime.now());

        String key = cardKey(card.getExternalCardId());
        String json = writeJson(card, key);
        Duration ttl = ttlUntil(card.getExpiresAt());
        if (ttl == null) {
            redisTemplate.opsForValue().set(key, json);
            redisTemplate.opsForValue().set(cardUidKey(card.getCardUid()), card.getExternalCardId());
        } else {
            redisTemplate.opsForValue().set(key, json, ttl);
            redisTemplate.opsForValue().set(cardUidKey(card.getCardUid()), card.getExternalCardId(), ttl);
        }
        redisTemplate.opsForSet().add(
                passengerCardsKey(card.getPassengerAccountId()), card.getExternalCardId());
        return card;
    }

    public List<CardResponse> getCardsForPassenger(String passengerAccountId) {
        String normalizedAccountId = requireText(passengerAccountId, "passenger account ID");
        if (!historyLoaded(passengerCardsLoadedKey(normalizedAccountId))) {
            level5Client.getCards(normalizedAccountId)
                    .forEach(card -> cacheHistoryCard(normalizedAccountId, card));
            markHistoryLoaded(passengerCardsLoadedKey(normalizedAccountId));
        }
        return readPassengerCards(normalizedAccountId);
    }

    private void ensureCardTypesLoaded() {
        if (catalogLoaded()) {
            return;
        }
        redisTemplate.delete(CARD_TYPE_CODES_KEY);
        cacheCardTypes(level5Client.getCardTypes().stream().map(this::toSyncRequest).toList());
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

    private Optional<CardType> getCardTypeFromRedis(String code) {
        return readJson(cardTypeKey(code), CardType.class);
    }

    private List<CardResponse> readPassengerCards(String passengerAccountId) {
        Set<String> cardIds = redisTemplate.opsForSet().members(passengerCardsKey(passengerAccountId));
        if (cardIds == null || cardIds.isEmpty()) {
            return List.of();
        }

        return cardIds.stream()
                .map(id -> readCard(cardKey(id)))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(
                        PhysicalCard::getIssuedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(CardResponse::from)
                .toList();
    }

    private void cacheHistoryCard(String passengerAccountId, ExternalCardHistoryResponse history) {
        PhysicalCard card = new PhysicalCard();
        card.setExternalCardId(history.getCardId());
        card.setPassengerAccountId(passengerAccountId);
        card.setCardUid(history.getCardUid());
        card.setStatus(history.getStatus() == null || history.getStatus().isBlank()
                ? "INACTIVE" : history.getStatus());
        card.setIssuedAt(history.getActivatedAt() == null ? history.getLinkedAt() : history.getActivatedAt());
        cacheCard(card);
    }

    private Optional<PhysicalCard> readCard(String key) {
        return readJson(key, PhysicalCard.class);
    }

    private <T> Optional<T> readJson(String key, Class<T> valueType) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, valueType));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to read Redis card cache entry: " + key, exception);
        }
    }

    private String writeJson(PhysicalCard card, String key) {
        try {
            return objectMapper.writeValueAsString(card);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to write Redis card cache entry: " + key, exception);
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
            throw new IllegalStateException("Unable to write Redis card cache entry: " + key, exception);
        }
    }

    private void validate(PhysicalCard card) {
        if (card == null) {
            throw new IllegalArgumentException("Card is required");
        }
        card.setExternalCardId(requireText(card.getExternalCardId(), "card ID"));
        card.setPassengerAccountId(requireText(card.getPassengerAccountId(), "passenger account ID"));
        card.setCardUid(requireText(card.getCardUid(), "card UID"));
        card.setStatus(requireText(card.getStatus(), "card status"));
    }

    private Duration ttlUntil(LocalDateTime expiresAt) {
        if (expiresAt == null) {
            return null;
        }
        long expiresAtEpoch = expiresAt.atZone(ZoneId.systemDefault()).toEpochSecond();
        long nowEpoch = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond();
        return Duration.ofSeconds(Math.max(expiresAtEpoch - nowEpoch, 1));
    }

    private String cardKey(String cardId) {
        return CARD_KEY_PREFIX + cardId;
    }

    private String cardUidKey(String cardUid) {
        return CARD_UID_KEY_PREFIX + cardUid;
    }

    private String passengerCardsKey(String passengerAccountId) {
        return PASSENGER_CARDS_KEY_PREFIX + passengerAccountId;
    }

    private String passengerCardsLoadedKey(String passengerAccountId) {
        return PASSENGER_CARDS_LOADED_KEY_PREFIX + passengerAccountId;
    }

    private boolean historyLoaded(String key) {
        return "true".equals(redisTemplate.opsForValue().get(key));
    }

    private void markHistoryLoaded(String key) {
        redisTemplate.opsForValue().set(key, "true", HISTORY_LOADED_TTL);
    }

    private boolean catalogLoaded() {
        return "true".equals(redisTemplate.opsForValue().get(CARD_TYPES_LOADED_KEY));
    }

    private void markCatalogLoaded(List<LocalDateTime> expirations) {
        Duration markerTtl = expirations.stream()
                .filter(Objects::nonNull)
                .map(this::ttlUntil)
                .filter(ttl -> !ttl.isNegative() && !ttl.isZero())
                .min(Duration::compareTo)
                .map(ttl -> ttl.compareTo(catalogRefreshTtl) < 0 ? ttl : catalogRefreshTtl)
                .orElse(catalogRefreshTtl);
        redisTemplate.opsForValue().set(CARD_TYPES_LOADED_KEY, "true", markerTtl);
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing " + fieldName + " in card data");
        }
        return value.trim();
    }

    private String cardTypeKey(String code) {
        return CARD_TYPE_KEY_PREFIX + Objects.requireNonNull(code);
    }
}
