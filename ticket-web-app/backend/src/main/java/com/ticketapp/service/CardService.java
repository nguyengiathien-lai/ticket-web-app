package com.ticketapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketapp.client.level5.Level5Client;
import com.ticketapp.dto.catalog.CardTypeSyncRequest;
import com.ticketapp.dto.card.CardResponse;
import com.ticketapp.dto.external.ExternalCardHistoryResponse;
import com.ticketapp.dto.external.ExternalCardTypeResponse;
import com.ticketapp.dto.purchase.CardTypeResponse;
import com.ticketapp.entity.CardType;
import com.ticketapp.entity.PhysicalCard;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CardService {

    private static final String CARD_KEY_PREFIX = "cache:cards:id:";
    private static final String CARD_UID_KEY_PREFIX = "cache:cards:uid:";
    private static final String PASSENGER_CARDS_KEY_PREFIX = "cache:cards:passenger:";
    private static final String PASSENGER_CARDS_LOADED_KEY_PREFIX = "cache:cards:loaded:";
    private static final String CARD_TYPE_KEY_PREFIX = "catalog:card-types:";
    private static final String CARD_TYPES_KEY = "catalog:card-types:codes";
    private static final String CARD_TYPES_LOADED_KEY = "catalog:card-types:loaded";
    private static final Duration HISTORY_LOADED_TTL = Duration.ofDays(3);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Level5Client level5Client;
    private final Duration catalogRefreshTtl;

    @Autowired
    public CardService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            Level5Client level5Client,
            @Value("${app.cache.catalog-refresh-ttl-seconds:600}") int catalogRefreshTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.level5Client = level5Client;
        this.catalogRefreshTtl = Duration.ofSeconds(catalogRefreshTtlSeconds);
    }

    public CardService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            Level5Client level5Client) {
        this(redisTemplate, objectMapper, level5Client, 600);
    }

    public List<CardTypeResponse> getActiveCardTypes() {
        refreshCardTypesIfEmpty();
        Set<String> codes = redisTemplate.opsForSet().members(CARD_TYPES_KEY);
        if (codes == null || codes.isEmpty()) {
            return List.of();
        }
        return codes.stream()
                .map(code -> readCardType(cardTypeKey(code)))
                .flatMap(Optional::stream)
                .filter(cardType -> Boolean.TRUE.equals(cardType.getActive()))
                .sorted(Comparator.comparing(
                        CardType::getCode,
                        Comparator.nullsLast(String::compareToIgnoreCase)))
                .map(CardTypeResponse::from)
                .toList();
    }

    public List<CardTypeResponse> cacheCardTypes(List<CardTypeSyncRequest> request) {
        if (request == null) {
            return List.of();
        }
        request.stream()
                .map(this::toCardType)
                .forEach(this::cacheCardType);
        return getActiveCardTypes();
    }

    public CardType requireActiveCardType(String code) {
        String normalizedCode = requireText(code, "card type code");
        refreshCardTypesIfEmpty();
        return readCardType(cardTypeKey(normalizedCode))
                .filter(cardType -> Boolean.TRUE.equals(cardType.getActive()))
                .orElseThrow(() -> new IllegalArgumentException("Card type not found or inactive"));
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

    private void refreshCardTypesIfEmpty() {
        if ("true".equals(redisTemplate.opsForValue().get(CARD_TYPES_LOADED_KEY))) {
            return;
        }
        level5Client.getCardTypes().stream()
                .map(this::toCardType)
                .forEach(this::cacheCardType);
        redisTemplate.opsForValue().set(CARD_TYPES_LOADED_KEY, "true", catalogRefreshTtl);
    }

    private CardType toCardType(CardTypeSyncRequest request) {
        CardType cardType = new CardType();
        cardType.setExternalCardTypeId(requireText(request.getExternalCardTypeId(), "card type ID"));
        cardType.setCode(requireText(request.getCode(), "card type code"));
        cardType.setName(requireText(request.getName(), "card type name"));
        cardType.setDurationDays(request.getDurationDays());
        cardType.setPrice(request.getPrice());
        cardType.setCurrency(requireText(request.getCurrency(), "card type currency"));
        cardType.setDescription(request.getDescription());
        cardType.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
        cardType.setCachedAt(LocalDateTime.now());
        cardType.setExpiresAt(request.getExpiresAt());
        return cardType;
    }

    private CardType toCardType(ExternalCardTypeResponse response) {
        CardType cardType = new CardType();
        cardType.setExternalCardTypeId(requireText(response.getExternalCardTypeId(), "card type ID"));
        cardType.setCode(requireText(response.getCode(), "card type code"));
        cardType.setName(requireText(response.getName(), "card type name"));
        cardType.setDurationDays(response.getDurationDays());
        cardType.setPrice(response.getPrice());
        cardType.setCurrency(requireText(response.getCurrency(), "card type currency"));
        cardType.setDescription(response.getDescription());
        cardType.setActive(response.getActive() == null ? Boolean.TRUE : response.getActive());
        cardType.setCachedAt(LocalDateTime.now());
        cardType.setExpiresAt(response.getExpiresAt());
        return cardType;
    }

    private void cacheCardType(CardType cardType) {
        String key = cardTypeKey(cardType.getCode());
        String json = writeJson(cardType, key);
        Duration ttl = ttlUntil(cardType.getExpiresAt());

        if (ttl == null) {
            redisTemplate.opsForValue().set(key, json);
        } else {
            redisTemplate.opsForValue().set(key, json, ttl);
        }
        redisTemplate.opsForSet().add(CARD_TYPES_KEY, cardType.getCode());
    }

    private Optional<CardType> readCardType(String key) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, CardType.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to read Redis card type cache entry: " + key, exception);
        }
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
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, PhysicalCard.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to read Redis card cache entry: " + key, exception);
        }
    }

    private String writeJson(CardType cardType, String key) {
        try {
            return objectMapper.writeValueAsString(cardType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to write Redis card type cache entry: " + key, exception);
        }
    }

    private String writeJson(PhysicalCard card, String key) {
        try {
            return objectMapper.writeValueAsString(card);
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

    private String cardTypeKey(String code) {
        return CARD_TYPE_KEY_PREFIX + code;
    }

    private boolean historyLoaded(String key) {
        return "true".equals(redisTemplate.opsForValue().get(key));
    }

    private void markHistoryLoaded(String key) {
        redisTemplate.opsForValue().set(key, "true", HISTORY_LOADED_TTL);
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing " + fieldName + " in card data");
        }
        return value.trim();
    }
}
