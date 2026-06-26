package com.validationgate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.validationgate.config.DeviceProperties;
import com.validationgate.dto.DeviceInformationPackageMessage;
import com.validationgate.entity.DeviceConfigPackage;
import com.validationgate.entity.MediaAccessRulesPackage;
import com.validationgate.entity.StationContextPackage;
import com.validationgate.repository.DeviceConfigPackageRepository;
import com.validationgate.repository.MediaAccessRulesPackageRepository;
import com.validationgate.repository.StationContextPackageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
        entity.setPayloadJson(toJson(payload));
        entity.setReceivedAt(receivedAt);
        mediaAccessRulesRepository.save(entity);
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
}
