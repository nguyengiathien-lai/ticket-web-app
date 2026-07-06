package com.validationgate.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.validationgate.entity.DeviceConfigPackage;
import com.validationgate.entity.MediaAccessRulesPackage;
import com.validationgate.entity.StationContextPackage;
import com.validationgate.repository.DeviceConfigPackageRepository;
import com.validationgate.repository.MediaAccessRulesPackageRepository;
import com.validationgate.repository.StationContextPackageRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(name = "app.example-package.enabled", havingValue = "true", matchIfMissing = true)
public class ExamplePackageDataInitializer implements CommandLineRunner {

    private static final String PACKAGE_ID = "example-package-v1";

    private final DeviceProperties deviceProperties;
    private final ObjectMapper objectMapper;
    private final DeviceConfigPackageRepository deviceConfigRepository;
    private final StationContextPackageRepository stationContextRepository;
    private final MediaAccessRulesPackageRepository mediaAccessRulesRepository;

    public ExamplePackageDataInitializer(
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

    @Override
    @Transactional
    public void run(String... args) {
        String deviceCode = deviceProperties.code();
        String stationCode = deviceProperties.stationCode();
        LocalDateTime receivedAt = LocalDateTime.now();

        deviceConfigRepository.findByStationCode(stationCode)
                .orElseGet(() -> deviceConfigRepository.save(deviceConfig(deviceCode, stationCode, receivedAt)));
        stationContextRepository.findByStationCode(stationCode)
                .orElseGet(() -> stationContextRepository.save(stationContext(deviceCode, stationCode, receivedAt)));
        mediaAccessRulesRepository.findByStationCode(stationCode)
                .orElseGet(() -> mediaAccessRulesRepository.save(mediaAccessRules(deviceCode, stationCode, receivedAt)));
    }

    private DeviceConfigPackage deviceConfig(String deviceCode, String stationCode, LocalDateTime receivedAt) {
        DeviceConfigPackage entity = new DeviceConfigPackage();
        entity.setPackageId(PACKAGE_ID);
        entity.setDeviceCode(deviceCode);
        entity.setStationCode(stationCode);
        entity.setPayloadJson(compactJson("""
                {
                  "deviceCode": "%s",
                  "stationCode": "%s",
                  "deviceType": "ENTRY_EXIT_GATE",
                  "firmwareVersion": "1.0.0-example",
                  "allowOfflineValidation": true,
                  "qrVerificationAlgorithm": "HMAC_SHA256",
                  "qrVerificationKey": "local-dev-qr-secret",
                  "maxClockDriftSeconds": 60,
                  "batchUploadEnabled": true,
                  "batchUploadIntervalSeconds": 30
                }
                """.formatted(deviceCode, stationCode)));
        entity.setReceivedAt(receivedAt);
        return entity;
    }

    private StationContextPackage stationContext(String deviceCode, String stationCode, LocalDateTime receivedAt) {
        StationContextPackage entity = new StationContextPackage();
        entity.setPackageId(PACKAGE_ID);
        entity.setDeviceCode(deviceCode);
        entity.setStationCode(stationCode);
        entity.setPayloadJson(compactJson("""
                {
                  "stationCode": "%s",
                  "stationName": "Example Central Station",
                  "lineCode": "BLUE",
                  "lineName": "Blue Line",
                  "zone": "A",
                  "timezone": "Asia/Bangkok",
                  "gates": [
                    {
                      "deviceCode": "%s",
                      "gateName": "Gate 1",
                      "direction": "BIDIRECTIONAL",
                      "platforms": ["1", "2"]
                    }
                  ]
                }
                """.formatted(stationCode, deviceCode)));
        entity.setReceivedAt(receivedAt);
        return entity;
    }

    private MediaAccessRulesPackage mediaAccessRules(String deviceCode, String stationCode, LocalDateTime receivedAt) {
        MediaAccessRulesPackage entity = new MediaAccessRulesPackage();
        entity.setPackageId(PACKAGE_ID);
        entity.setDeviceCode(deviceCode);
        entity.setStationCode(stationCode);
        entity.setPayloadJson(compactJson("""
                {
                  "stationCode": "%s",
                  "acceptedMedia": ["QR_TICKET", "TRANSIT_CARD"],
                  "qrTicket": {
                    "enabled": true,
                    "maxFutureValiditySeconds": 86400,
                    "allowSingleUseOnly": true
                  },
                  "transitCard": {
                    "enabled": true,
                    "minimumBalance": 1000,
                    "currency": "VND"
                  },
                  "fareRules": [
                    {
                      "mediaType": "QR_TICKET",
                      "eventTypes": ["ENTRY", "EXIT"],
                      "allowed": true
                    },
                    {
                      "mediaType": "TRANSIT_CARD",
                      "eventTypes": ["ENTRY", "EXIT"],
                      "allowed": true
                    }
                  ]
                }
                """.formatted(stationCode)));
        entity.setReceivedAt(receivedAt);
        return entity;
    }

    private String compactJson(String json) {
        try {
            return objectMapper.writeValueAsString(objectMapper.readTree(json));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Example package JSON is invalid", exception);
        }
    }
}
