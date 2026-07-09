package com.ticketapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketapp.client.level4.Level4Client;
import com.ticketapp.dto.external.ExternalTicketResponse;
import com.ticketapp.dto.external.ExternalTicketHistoryResponse;
import com.ticketapp.dto.external.QrCodeRequest;
import com.ticketapp.dto.external.QrCodeResponse;
import com.ticketapp.dto.fare.FarePackageResponse;
import com.ticketapp.dto.ticket.TicketQrResponse;
import com.ticketapp.dto.ticket.TicketRequest;
import com.ticketapp.dto.ticket.TicketResponse;
import com.ticketapp.client.level5.Level5Client;
import com.ticketapp.entity.FarePackage;
import com.ticketapp.entity.Ticket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class TicketService {

    private static final String TICKET_KEY_PREFIX = "cache:tickets:id:";
    private static final String TICKET_CODE_KEY_PREFIX = "cache:tickets:code:";
    private static final String PASSENGER_TICKETS_KEY_PREFIX = "cache:tickets:passenger:";
    private static final String PASSENGER_TICKETS_LOADED_KEY_PREFIX = "cache:tickets:loaded:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Level5Client level5Client;
    private final Level4Client level4Client;
    private final FarePackageService farePackageService;
    private final Duration ticketCacheTtl;

    @Autowired
    public TicketService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            Level5Client level5Client,
            Level4Client level4Client,
            FarePackageService farePackageService,
            @Value("${app.cache.user-data-ttl-seconds:1800}") int userDataCacheTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.level5Client = level5Client;
        this.level4Client = level4Client;
        this.farePackageService = farePackageService;
        this.ticketCacheTtl = Duration.ofSeconds(userDataCacheTtlSeconds);
    }

    public TicketService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            Level5Client level5Client,
            Level4Client level4Client,
            FarePackageService farePackageService) {
        this(redisTemplate, objectMapper, level5Client, level4Client, farePackageService, 1800);
    }

    public TicketService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            Level5Client level5Client,
            Level4Client level4Client) {
        this(redisTemplate, objectMapper, level5Client, level4Client, null, 1800);
    }

    public TicketService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            Level5Client level5Client,
            Level4Client level4Client,
            int userDataCacheTtlSeconds) {
        this(redisTemplate, objectMapper, level5Client, level4Client, null, userDataCacheTtlSeconds);
    }

    public List<FarePackageResponse> getActiveFarePackages() {
        return requireFarePackageService().getActiveFarePackages();
    }

    public FarePackage requireActiveFarePackage(String code) {
        return requireFarePackageService().requireActiveFarePackage(code);
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
        List<TicketResponse> cachedTickets = readPassengerTickets(normalizedAccountId);
        if (!cachedTickets.isEmpty()) {
            return cachedTickets;
        }

        if (!historyLoaded(passengerTicketsLoadedKey(normalizedAccountId))) {
            try {
                level5Client.getTickets(normalizedAccountId)
                        .forEach(ticket -> cacheHistoryTicket(normalizedAccountId, ticket));
                markHistoryLoaded(passengerTicketsLoadedKey(normalizedAccountId));
            } catch (RuntimeException exception) {
                throw exception;
            }
        }
        return readPassengerTickets(normalizedAccountId);
    }

    public TicketQrResponse getTicketQrCode(String passengerAccountId, String ticketId) {
        String normalizedTicketId = requireText(ticketId, "ticket ID");
        QrCodeResponse qrCode = level4Client.generateQrCode(new QrCodeRequest(normalizedTicketId));
        if (qrCode == null || qrCode.getQrCode() == null || qrCode.getQrCode().isBlank()) {
            throw new IllegalStateException("Level 4 returned an incomplete QR code");
        }
        return new TicketQrResponse(normalizedTicketId, qrCode.getQrCode());
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
        external.setMode(history.getMode());
        external.setScope(history.getScope());
        external.setRouteId(history.getRouteId());
        external.setFare(history.getPrice());
        external.setCurrency("VND");
        external.setFromStationCode(history.getFromStationCode());
        external.setToStationCode(history.getToStationCode());
        external.setValidFrom(history.getValidFrom() == null ? null : history.getValidFrom().atStartOfDay());
        external.setValidUntil(history.getValidTo() == null ? null : history.getValidTo().atTime(23, 59, 59));
        external.setQrToken(history.getQrToken());
        external.setExpired(history.isExpired());
        external.setIssuedAt(history.getPurchasedAt());
        external.setExpiresAt(external.getValidUntil());
        cacheExternalTicket(request, external);
    }

    private void cacheTicket(Ticket ticket) {
        String key = ticketKey(ticket.getExternalTicketId());
        String json = writeJson(ticket, key);
        Duration ttl = effectiveTtl(ticket.getExpiresAt(), ticketCacheTtl);

        redisTemplate.opsForValue().set(key, json, ttl);
        redisTemplate.opsForValue().set(
                ticketCodeKey(ticket.getTicketCode()), ticket.getExternalTicketId(), ttl);

        String passengerTicketsKey = passengerTicketsKey(ticket.getPassengerAccountId());
        redisTemplate.opsForSet().add(passengerTicketsKey, ticket.getExternalTicketId());
        redisTemplate.expire(passengerTicketsKey, ticketCacheTtl);
    }

    private Optional<Ticket> readTicket(String key) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, Ticket.class));
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
        Boolean expired = firstValue(externalTicket.getExpired(), ticket.getExpired());

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
        ticket.setMode(firstText(externalTicket.getMode(), ticket.getMode()));
        ticket.setScope(firstText(externalTicket.getScope(), ticket.getScope()));
        ticket.setRouteId(firstText(externalTicket.getRouteId(), ticket.getRouteId()));
        ticket.setFare(fare);
        ticket.setCurrency(firstText(externalTicket.getCurrency(), ticket.getCurrency(), "VND"));
        ticket.setFromStationCode(firstText(externalTicket.getFromStationCode(), ticket.getFromStationCode()));
        ticket.setToStationCode(firstText(externalTicket.getToStationCode(), ticket.getToStationCode()));
        ticket.setValidFrom(validFrom);
        ticket.setValidUntil(validUntil);
        ticket.setQrToken(firstText(externalTicket.getQrToken(), ticket.getQrToken(), externalTicketId));
        ticket.setExpired(firstValue(expired, validUntil == null ? null : validUntil.isBefore(LocalDateTime.now())));
        // ticket.setRemainingUses(remainingUses);
        ticket.setIssuedAt(firstValue(externalTicket.getIssuedAt(), ticket.getIssuedAt(), now));
        ticket.setCachedAt(now);
        ticket.setExpiresAt(firstValue(externalTicket.getExpiresAt(), ticket.getExpiresAt(), validUntil));
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

    private boolean historyLoaded(String key) {
        return "true".equals(redisTemplate.opsForValue().get(key));
    }

    private void markHistoryLoaded(String key) {
        redisTemplate.opsForValue().set(key, "true", ticketCacheTtl);
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing " + fieldName + " in ticket data");
        }
        return value.trim();
    }

    private FarePackageService requireFarePackageService() {
        if (farePackageService == null) {
            throw new IllegalStateException("Fare package service is required");
        }
        return farePackageService;
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
}
