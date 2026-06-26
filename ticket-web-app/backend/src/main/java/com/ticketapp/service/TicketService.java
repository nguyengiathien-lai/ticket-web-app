package com.ticketapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketapp.client.level4.Level4Client;
import com.ticketapp.client.level5.Level5Client;
import com.ticketapp.dto.catalog.TicketTypeSyncRequest;
import com.ticketapp.dto.external.ExternalTicketTypeResponse;
import com.ticketapp.dto.external.ExternalTicketResponse;
import com.ticketapp.dto.external.ExternalTicketHistoryResponse;
import com.ticketapp.dto.external.QrCodeRequest;
import com.ticketapp.dto.external.QrCodeResponse;
import com.ticketapp.dto.ticket.TicketQrResponse;
import com.ticketapp.dto.ticket.TicketRequest;
import com.ticketapp.dto.ticket.TicketResponse;
import com.ticketapp.dto.ticket.TicketTypeResponse;
import com.ticketapp.entity.Ticket;
import com.ticketapp.entity.TicketType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class TicketService {

    private static final String TICKET_KEY_PREFIX = "cache:tickets:id:";
    private static final String TICKET_CODE_KEY_PREFIX = "cache:tickets:code:";
    private static final String PASSENGER_TICKETS_KEY_PREFIX = "cache:tickets:passenger:";
    private static final String PASSENGER_TICKETS_LOADED_KEY_PREFIX = "cache:tickets:loaded:";
    private static final Duration HISTORY_LOADED_TTL = Duration.ofMinutes(10);
    private static final String TICKET_TYPE_CODES_KEY = "catalog:ticket-types:codes";
    private static final String TICKET_TYPE_KEY_PREFIX = "catalog:ticket-types:";
    private static final String TICKET_TYPES_LOADED_KEY = "catalog:ticket-types:loaded";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Level5Client level5Client;
    private final Level4Client level4Client;
    private final Duration catalogRefreshTtl;

    public TicketService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            Level5Client level5Client,
            Level4Client level4Client,
            @Value("${app.cache.catalog-refresh-ttl-seconds:600}") long catalogRefreshTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.level5Client = level5Client;
        this.level4Client = level4Client;
        this.catalogRefreshTtl = Duration.ofSeconds(Math.max(catalogRefreshTtlSeconds, 1));
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

    public TicketType requireActiveTicketType(String code) {
        ensureTicketTypesLoaded();
        TicketType ticketType = getTicketTypeFromRedis(code)
                .orElseThrow(() -> new IllegalArgumentException("Ticket type not found in Redis cache: " + code));

        if (!Boolean.TRUE.equals(ticketType.getActive())) {
            throw new IllegalArgumentException("Ticket type is inactive: " + code);
        }

        return ticketType;
    }

    public List<TicketTypeResponse> cacheTicketTypes(List<TicketTypeSyncRequest> requests) {
        List<TicketType> cached = requests.stream()
                .map(this::cacheTicketType)
                .toList();
        markCatalogLoaded(requests.stream()
                .map(TicketTypeSyncRequest::getExpiresAt)
                .toList());
        return cached.stream()
                .map(TicketTypeResponse::from)
                .toList();
    }

    public TicketResponse cacheExternalTicket(TicketRequest request, ExternalTicketResponse externalTicket) {
        if (request == null || externalTicket == null) {
            throw new IllegalStateException("External ticket system returned an incomplete ticket");
        }
        String externalTicketId = requireText(externalTicket.getExternalTicketId(), "ticket ID");
        Ticket ticket = readTicket(ticketKey(externalTicketId)).orElseGet(Ticket::new);

        applyExternalTicket(ticket, request, externalTicket);
        cacheTicket(ticket);
        return TicketResponse.from(ticket);
    }

    public TicketResponse getCachedTicketByCode(String ticketCode) {
        String normalizedCode = requireText(ticketCode, "ticket code");
        String externalTicketId = redisTemplate.opsForValue().get(ticketCodeKey(normalizedCode));
        if (externalTicketId == null || externalTicketId.isBlank()) {
            throw new IllegalArgumentException("Ticket not found");
        }
        return readTicket(ticketKey(externalTicketId))
                .map(TicketResponse::from)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
    }

    public List<TicketResponse> getTicketsForPassenger(String passengerAccountId) {
        String normalizedAccountId = requireText(passengerAccountId, "passenger account ID");
        if (!historyLoaded(passengerTicketsLoadedKey(normalizedAccountId))) {
            level5Client.getTickets(normalizedAccountId)
                    .forEach(ticket -> cacheHistoryTicket(normalizedAccountId, ticket));
            markHistoryLoaded(passengerTicketsLoadedKey(normalizedAccountId));
        }
        return readPassengerTickets(normalizedAccountId);
    }

    public TicketQrResponse getTicketQrCode(String passengerAccountId, String ticketId) {
        String normalizedAccountId = requireText(passengerAccountId, "passenger account ID");
        String normalizedTicketId = requireText(ticketId, "ticket ID");
        Ticket ticket = readTicket(ticketKey(normalizedTicketId))
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

        if (!normalizedAccountId.equals(ticket.getPassengerAccountId())) {
            throw new SecurityException("Ticket does not belong to this account");
        }

        QrCodeResponse qrCode = level4Client.generateQrCode(new QrCodeRequest(ticket.getExternalTicketId()));
        if (qrCode == null || qrCode.getQrCode() == null || qrCode.getQrCode().isBlank()) {
            throw new IllegalStateException("Level 4 returned an incomplete QR code");
        }
        return new TicketQrResponse(ticket.getExternalTicketId(), qrCode.getQrCode());
    }

    private void ensureTicketTypesLoaded() {
        if (catalogLoaded()) {
            return;
        }
        redisTemplate.delete(TICKET_TYPE_CODES_KEY);
        cacheTicketTypes(level5Client.getTicketTypes().stream().map(this::toSyncRequest).toList());
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

    private Optional<TicketType> getTicketTypeFromRedis(String code) {
        return readJson(ticketTypeKey(code), TicketType.class);
    }

    private List<TicketResponse> readPassengerTickets(String passengerAccountId) {
        Set<String> ticketIds = redisTemplate.opsForSet().members(passengerTicketsKey(passengerAccountId));
        if (ticketIds == null || ticketIds.isEmpty()) {
            return List.of();
        }

        return ticketIds.stream()
                .map(id -> readTicket(ticketKey(id)))
                .flatMap(Optional::stream)
                .sorted(Comparator.comparing(
                        Ticket::getIssuedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(TicketResponse::from)
                .toList();
    }

    private void cacheHistoryTicket(String passengerAccountId, ExternalTicketHistoryResponse history) {
        TicketRequest request = new TicketRequest();
        request.setPassengerAccountId(passengerAccountId);
        request.setTicketTypeCode(firstText(history.getType(), "UNKNOWN"));

        ExternalTicketResponse external = new ExternalTicketResponse();
        external.setExternalTicketId(history.getTicketId());
        external.setPassengerAccountId(passengerAccountId);
        external.setTicketTypeCode(request.getTicketTypeCode());
        external.setStatus(history.getStatus());
        external.setFare(history.getPrice());
        external.setCurrency("VND");
        external.setValidFrom(history.getValidFrom() == null ? null : history.getValidFrom().atStartOfDay());
        external.setValidUntil(history.getValidTo() == null ? null : history.getValidTo().atTime(23, 59, 59));
        external.setIssuedAt(history.getPurchasedAt());
        external.setExpiresAt(external.getValidUntil());
        cacheExternalTicket(request, external);
    }

    private void cacheTicket(Ticket ticket) {
        String key = ticketKey(ticket.getExternalTicketId());
        String json = writeJson(ticket, key);
        Duration ttl = ttlUntil(ticket.getExpiresAt());

        if (ttl == null) {
            redisTemplate.opsForValue().set(key, json);
            redisTemplate.opsForValue().set(ticketCodeKey(ticket.getTicketCode()), ticket.getExternalTicketId());
        } else {
            redisTemplate.opsForValue().set(key, json, ttl);
            redisTemplate.opsForValue().set(
                    ticketCodeKey(ticket.getTicketCode()), ticket.getExternalTicketId(), ttl);
        }
        redisTemplate.opsForSet().add(
                passengerTicketsKey(ticket.getPassengerAccountId()), ticket.getExternalTicketId());
    }

    private Optional<Ticket> readTicket(String key) {
        return readJson(key, Ticket.class);
    }

    private <T> Optional<T> readJson(String key, Class<T> valueType) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, valueType));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to read Redis ticket cache entry: " + key, exception);
        }
    }

    private String writeJson(Ticket ticket, String key) {
        try {
            return objectMapper.writeValueAsString(ticket);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to write Redis ticket cache entry: " + key, exception);
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
            throw new IllegalStateException("Unable to write Redis ticket cache entry: " + key, exception);
        }
    }

    private void applyExternalTicket(
            Ticket ticket,
            TicketRequest request,
            ExternalTicketResponse externalTicket) {
        LocalDateTime now = LocalDateTime.now();

        String externalTicketId = requireText(externalTicket.getExternalTicketId(), "ticket ID");
        String passengerAccountId = requireText(firstText(
                externalTicket.getPassengerAccountId(), request.getPassengerAccountId(),
                ticket.getPassengerAccountId()), "passenger account ID");
        String ticketTypeCode = requireText(firstText(
                externalTicket.getTicketTypeCode(), request.getTicketTypeCode(),
                ticket.getTicketTypeCode()), "ticket type code");
        BigDecimal fare = firstValue(externalTicket.getFare(), ticket.getFare());
        // Integer remainingUses = firstValue(externalTicket.getRemainingUses(), ticket.getRemainingUses());
        LocalDateTime validFrom = firstValue(externalTicket.getValidFrom(), ticket.getValidFrom());
        LocalDateTime validUntil = firstValue(externalTicket.getValidUntil(), ticket.getValidUntil());

        if (fare != null && fare.signum() < 0) {
            throw new IllegalStateException("Level 5 returned a negative ticket fare");
        }
        // if (remainingUses != null && remainingUses < 0) {
        //     throw new IllegalStateException("Level 5 returned negative remaining uses");
        // }
        if (validFrom != null && validUntil != null && validUntil.isBefore(validFrom)) {
            throw new IllegalStateException("Level 5 returned an invalid ticket validity period");
        }

        ticket.setExternalTicketId(externalTicketId);
        ticket.setPassengerAccountId(passengerAccountId);
        ticket.setTicketTypeCode(ticketTypeCode);
        ticket.setPhysicalCardExternalId(firstText(
                externalTicket.getPhysicalCardExternalId(), request.getPhysicalCardExternalId(),
                ticket.getPhysicalCardExternalId()));
        ticket.setTicketCode(firstText(externalTicket.getTicketCode(), ticket.getTicketCode(), externalTicketId));
        ticket.setStatus(firstText(externalTicket.getStatus(), ticket.getStatus(), "ACTIVE"));
        ticket.setFare(fare);
        ticket.setCurrency(firstText(externalTicket.getCurrency(), ticket.getCurrency(), "VND"));
        ticket.setValidFrom(validFrom);
        ticket.setValidUntil(validUntil);
        // ticket.setRemainingUses(remainingUses);
        ticket.setIssuedAt(firstValue(externalTicket.getIssuedAt(), ticket.getIssuedAt(), now));
        ticket.setCachedAt(now);
        ticket.setExpiresAt(firstValue(externalTicket.getExpiresAt(), ticket.getExpiresAt(), validUntil));
    }

    private Duration ttlUntil(LocalDateTime expiresAt) {
        if (expiresAt == null) {
            return null;
        }
        long expiresAtEpoch = expiresAt.atZone(ZoneId.systemDefault()).toEpochSecond();
        long nowEpoch = LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond();
        return Duration.ofSeconds(Math.max(expiresAtEpoch - nowEpoch, 1));
    }

    private String ticketKey(String ticketId) {
        return TICKET_KEY_PREFIX + ticketId;
    }

    private String ticketCodeKey(String ticketCode) {
        return TICKET_CODE_KEY_PREFIX + ticketCode;
    }

    private String passengerTicketsKey(String passengerAccountId) {
        return PASSENGER_TICKETS_KEY_PREFIX + passengerAccountId;
    }

    private String passengerTicketsLoadedKey(String passengerAccountId) {
        return PASSENGER_TICKETS_LOADED_KEY_PREFIX + passengerAccountId;
    }

    private boolean historyLoaded(String key) {
        return "true".equals(redisTemplate.opsForValue().get(key));
    }

    private void markHistoryLoaded(String key) {
        redisTemplate.opsForValue().set(key, "true", HISTORY_LOADED_TTL);
    }

    private boolean catalogLoaded() {
        return "true".equals(redisTemplate.opsForValue().get(TICKET_TYPES_LOADED_KEY));
    }

    private void markCatalogLoaded(List<LocalDateTime> expirations) {
        Duration markerTtl = expirations.stream()
                .filter(Objects::nonNull)
                .map(this::ttlUntil)
                .filter(ttl -> !ttl.isNegative() && !ttl.isZero())
                .min(Duration::compareTo)
                .map(ttl -> ttl.compareTo(catalogRefreshTtl) < 0 ? ttl : catalogRefreshTtl)
                .orElse(catalogRefreshTtl);
        redisTemplate.opsForValue().set(TICKET_TYPES_LOADED_KEY, "true", markerTtl);
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing " + fieldName + " in ticket data");
        }
        return value.trim();
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
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

    private String ticketTypeKey(String code) {
        return TICKET_TYPE_KEY_PREFIX + Objects.requireNonNull(code);
    }
}
