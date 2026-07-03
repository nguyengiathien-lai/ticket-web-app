package com.validationgate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.validationgate.config.DeviceProperties;
import com.validationgate.dto.DeviceInformationPackageMessage;
import com.validationgate.entity.DeviceConfigPackage;
import com.validationgate.entity.MediaAccessCardStatusRule;
import com.validationgate.entity.MediaAccessRulesPackage;
import com.validationgate.entity.StationContextPackage;
import com.validationgate.repository.DeviceConfigPackageRepository;
import com.validationgate.repository.MediaAccessRulesPackageRepository;
import com.validationgate.repository.StationContextPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DeviceInformationPackageService {

    private final DeviceProperties deviceProperties;
    private final ObjectMapper objectMapper;
    private final DeviceConfigPackageRepository deviceConfigRepository;
    private final StationContextPackageRepository stationContextRepository;
    private final MediaAccessRulesPackageRepository mediaAccessRulesRepository;

    public DeviceInformationPackageService(
            DeviceProperties deviceProperties,
            ObjectMapper objectMapper,
            DeviceConfigPackageRepository deviceConfigRepository,
            StationContextPackageRepository stationContextRepository,
            MediaAccessRulesPackageRepository mediaAccessRulesRepository) {
        this.deviceProperties = deviceProperties;
        this.objectMapper = objectMapper;
        this.deviceConfigRepository = deviceConfigRepository;
        this.stationContextRepository = stationContextRepository;
        this.mediaAccessRulesRepository = mediaAccessRulesRepository;
    }

    @Transactional
    public void storePackage(DeviceInformationPackageMessage message) {
        requirePackageForThisStation(message);

        String stationCode = stationCode(message).trim();
        String packageId = hasText(message.publishedAt()) ? message.publishedAt().trim() : UUID.randomUUID().toString();
        String deviceCode = deviceProperties.code();
        LocalDateTime receivedAt = LocalDateTime.now();

        upsertDeviceConfig(packageId, deviceCode, stationCode, message.deviceConfig(), receivedAt);
        upsertStationContext(packageId, deviceCode, stationCode, message.stationContext(), receivedAt);
        upsertMediaAccessRules(packageId, deviceCode, stationCode, message.mediaAccessRules(), receivedAt);
    }

    private void requirePackageForThisStation(DeviceInformationPackageMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("Device information package is required");
        }
        if (message.deviceConfig() == null || message.stationContext() == null || message.mediaAccessRules() == null) {
            throw new IllegalArgumentException("Device information package must include deviceConfig, stationContext, and mediaAccessRules");
        }
        if (!hasText(stationCode(message))) {
            throw new IllegalArgumentException("Device information package stationContext.stationCode is required");
        }
        if (!deviceProperties.stationCode().equals(stationCode(message))) {
            throw new IllegalArgumentException("Package station code does not match this gate device");
        }
    }

    private void upsertDeviceConfig(
            String packageId, String deviceCode, String stationCode, JsonNode payload, LocalDateTime receivedAt) {
        DeviceConfigPackage entity = deviceConfigRepository.findByStationCode(stationCode)
                .orElseGet(DeviceConfigPackage::new);
        entity.setPackageId(packageId);
        entity.setDeviceCode(deviceCode);
        entity.setStationCode(stationCode);
        entity.setVersion(integer(payload, "version"));
        entity.setMaxOfflineSeconds(integer(payload, "maxOfflineSeconds"));
        entity.setAllowOfflineValidation(booleanValue(payload, "allowOfflineValidation"));
        entity.setDeviceTypes(toJson(payload.path("deviceTypes")));
        entity.setQrVerificationAlgorithm(text(payload, "qrVerificationAlgorithm"));
        entity.setQrVerificationKey(text(payload, "qrVerificationKey"));
        entity.setQrMaxTtlSeconds(integer(payload, "qrMaxTtlSeconds"));
        entity.setMaxClockDriftSeconds(integer(payload, "maxClockDriftSeconds"));
        entity.setHeartbeatIntervalSeconds(integer(payload, "heartbeatIntervalSeconds"));
        entity.setPayloadJson(toJson(payload));
        entity.setReceivedAt(receivedAt);
        deviceConfigRepository.save(entity);
    }

    private void upsertStationContext(
            String packageId, String deviceCode, String stationCode, JsonNode payload, LocalDateTime receivedAt) {
        StationContextPackage entity = stationContextRepository.findByStationCode(stationCode)
                .orElseGet(StationContextPackage::new);
        entity.setPackageId(packageId);
        entity.setDeviceCode(deviceCode);
        entity.setStationCode(stationCode);
        entity.setStationName(text(payload, "stationName"));
        entity.setRouteCode(text(payload, "routeCode"));
        entity.setStationOrder(integer(payload, "stationOrder"));
        entity.setDistance(decimal(payload, "distance"));
        entity.setOperatorCode(text(payload, "operatorCode"));
        entity.setPayloadJson(toJson(payload));
        entity.setReceivedAt(receivedAt);
        stationContextRepository.save(entity);
    }

    private void upsertMediaAccessRules(
            String packageId, String deviceCode, String stationCode, JsonNode payload, LocalDateTime receivedAt) {
        MediaAccessRulesPackage entity = mediaAccessRulesRepository.findByStationCode(stationCode)
                .orElseGet(MediaAccessRulesPackage::new);
        entity.setPackageId(packageId);
        entity.setDeviceCode(deviceCode);
        entity.setStationCode(stationCode);
        entity.setVersion(integer(payload, "version"));
        entity.setPayloadJson(toJson(payload));
        entity.setReceivedAt(receivedAt);
        entity.replaceCardStatusRules(cardStatusRules(payload));
        mediaAccessRulesRepository.save(entity);
    }

    private List<MediaAccessCardStatusRule> cardStatusRules(JsonNode payload) {
        List<MediaAccessCardStatusRule> rules = new ArrayList<>();
        JsonNode ruleNodes = payload.path("cardStatusRules");
        if (!ruleNodes.isArray()) {
            return rules;
        }
        for (JsonNode ruleNode : ruleNodes) {
            String cardId = text(ruleNode, "cardId");
            String status = text(ruleNode, "status");
            if (!hasText(cardId) || !hasText(status)) {
                continue;
            }
            MediaAccessCardStatusRule rule = new MediaAccessCardStatusRule();
            rule.setCardId(cardId);
            rule.setStatus(status);
            rule.setStatusReason(text(ruleNode, "statusReason"));
            rule.setRuleUpdatedAt(dateTime(ruleNode, "updatedAt"));
            rules.add(rule);
        }
        return rules;
    }

    private String toJson(JsonNode payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Could not serialize package section", exception);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String stationCode(DeviceInformationPackageMessage message) {
        JsonNode stationCode = message.stationContext().get("stationCode");
        return stationCode == null || stationCode.isNull() ? null : stationCode.asText();
    }

    private String text(JsonNode payload, String fieldName) {
        JsonNode value = payload.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }

    private Integer integer(JsonNode payload, String fieldName) {
        JsonNode value = payload.get(fieldName);
        return value == null || value.isNull() || !value.canConvertToInt() ? null : value.asInt();
    }

    private Boolean booleanValue(JsonNode payload, String fieldName) {
        JsonNode value = payload.get(fieldName);
        return value == null || value.isNull() ? null : value.asBoolean();
    }

    private BigDecimal decimal(JsonNode payload, String fieldName) {
        JsonNode value = payload.get(fieldName);
        return value == null || value.isNull() || !value.isNumber() ? null : value.decimalValue();
    }

    private LocalDateTime dateTime(JsonNode payload, String fieldName) {
        String value = text(payload, fieldName);
        if (!hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (RuntimeException ignored) {
            try {
                return LocalDateTime.ofInstant(Instant.parse(value), ZoneId.systemDefault());
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("Invalid datetime field " + fieldName, exception);
            }
        }
    }
}
