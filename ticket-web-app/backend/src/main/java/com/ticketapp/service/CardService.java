package com.ticketapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketapp.client.level5.Level5Client;
import com.ticketapp.dto.card.CardResponse;
import com.ticketapp.dto.external.ExternalCardHistoryResponse;
import com.ticketapp.dto.fare.FarePackageResponse;
import com.ticketapp.entity.FarePackage;
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

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Level5Client level5Client;
    private final FarePackageService farePackageService;
    private final Duration cardCacheTtl;

    @Autowired
    public CardService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            Level5Client level5Client,
            FarePackageService farePackageService,
            @Value("${app.cache.user-data-ttl-seconds:1800}") int userDataCacheTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.level5Client = level5Client;
        this.farePackageService = farePackageService;
        this.cardCacheTtl = Duration.ofSeconds(userDataCacheTtlSeconds);
    }

    public CardService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            Level5Client level5Client,
            FarePackageService farePackageService) {
        this(redisTemplate, objectMapper, level5Client, farePackageService, 1800);
    }

    public CardService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            Level5Client level5Client) {
        this(redisTemplate, objectMapper, level5Client, null, 1800);
    }

    public CardService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            Level5Client level5Client,
            int userDataCacheTtlSeconds) {
        this(redisTemplate, objectMapper, level5Client, null, userDataCacheTtlSeconds);
    }

    public List<FarePackageResponse> getActiveFarePackages() {
        return requireFarePackageService().getActiveFarePackages();
    }

    public FarePackage requireActiveFarePackage(String code) {
        return requireFarePackageService().requireActiveFarePackage(code);
    }

    public PhysicalCard cacheCard(PhysicalCard card) {
        validate(card);
        card.setCachedAt(LocalDateTime.now());

        String key = cardKey(card.getExternalCardId());
        String json = writeJson(card, key);
        Duration ttl = effectiveTtl(card.getExpiresAt(), cardCacheTtl);
        redisTemplate.opsForValue().set(key, json, ttl);
        redisTemplate.opsForValue().set(cardUidKey(card.getCardUid()), card.getExternalCardId(), ttl);

        String passengerCardsKey = passengerCardsKey(card.getPassengerAccountId());
        redisTemplate.opsForSet().add(passengerCardsKey, card.getExternalCardId());
        redisTemplate.expire(passengerCardsKey, cardCacheTtl);
        return card;
    }

    public List<CardResponse> getCardsForPassenger(String passengerAccountId) {
        String normalizedAccountId = requireText(passengerAccountId, "passenger account ID");
        List<CardResponse> cachedCards = readPassengerCards(normalizedAccountId);
        if (!cachedCards.isEmpty()) {
            return cachedCards;
        }

        if (!historyLoaded(passengerCardsLoadedKey(normalizedAccountId))) {
            try {
                level5Client.getCards(normalizedAccountId)
                        .forEach(card -> cacheHistoryCard(normalizedAccountId, card));
                markHistoryLoaded(passengerCardsLoadedKey(normalizedAccountId));
            } catch (RuntimeException exception) {
                throw exception;
            }
        }
        return readPassengerCards(normalizedAccountId);
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

    private Duration effectiveTtl(LocalDateTime expiresAt, Duration defaultTtl) {
        Duration expiryTtl = ttlUntil(expiresAt);
        if (expiryTtl == null || expiryTtl.compareTo(defaultTtl) > 0) {
            return defaultTtl;
        }
        return expiryTtl;
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
        redisTemplate.opsForValue().set(key, "true", cardCacheTtl);
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing " + fieldName + " in card data");
        }
        return value.trim();
    }

    private FarePackageService requireFarePackageService() {
        if (farePackageService == null) {
            throw new IllegalStateException("Fare package service is required");
        }
        return farePackageService;
    }
}
