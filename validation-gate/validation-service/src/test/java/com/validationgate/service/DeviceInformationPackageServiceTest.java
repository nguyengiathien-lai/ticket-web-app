package com.validationgate.service;

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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeviceInformationPackageServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void storesCombinedPackageAsThreeLocalSections() throws Exception {
        DeviceConfigPackageRepository deviceConfigRepository = mock(DeviceConfigPackageRepository.class);
        StationContextPackageRepository stationContextRepository = mock(StationContextPackageRepository.class);
        MediaAccessRulesPackageRepository mediaAccessRulesRepository = mock(MediaAccessRulesPackageRepository.class);
        when(deviceConfigRepository.findByStationAndDeviceCode("station-1", "device-1")).thenReturn(Optional.empty());
        when(stationContextRepository.findByStationCode("station-1")).thenReturn(Optional.empty());
        when(mediaAccessRulesRepository.findByStationCode("station-1")).thenReturn(Optional.empty());
        DeviceInformationPackageService service = new DeviceInformationPackageService(
                new DeviceProperties("device-1", "station-1", null),
                objectMapper,
                deviceConfigRepository,
                stationContextRepository,
                mediaAccessRulesRepository);
        DeviceInformationPackageMessage message = new DeviceInformationPackageMessage(
                1001L,
                "2026-06-25T00:00:00Z",
                objectMapper.readTree("""
                        {
                          "version": 13,
                          "maxOfflineSeconds": 60,
                          "allowOfflineValidation": true,
                          "deviceTypes": ["QR_SCANNER_SIMULATOR"],
                          "qrVerificationAlgorithm": "HMAC_SHA256",
                          "qrVerificationKey": "base64url-encoded-secret",
                          "qrMaxTtlSeconds": 30,
                          "maxClockDriftSeconds": 30,
                          "heartbeatIntervalSeconds": 30
                        }
                        """),
                objectMapper.readTree("""
                        {
                          "stationCode": "station-1",
                          "stationName": "Central",
                          "routeCode": "METRO-001",
                          "stationOrder": 1,
                          "distance": 0.0,
                          "operatorCode": "HCMC-METRO"
                        }
                        """),
                objectMapper.readTree("""
                        {
                          "version": 5,
                          "cardStatusRules": [
                            {
                              "cardId": "uuid-1",
                              "status": "BLACKLISTED",
                              "statusReason": "LOST_CARD",
                              "updatedAt": "2026-06-25T00:00:00"
                            },
                            {
                              "cardId": "uuid-2",
                              "status": "CANCELLED",
                              "statusReason": "FRAUD",
                              "updatedAt": "2026-06-25T00:00:00"
                            }
                          ]
                        }
                        """));

        service.storePackage(message);

        ArgumentCaptor<DeviceConfigPackage> deviceConfigCaptor = ArgumentCaptor.forClass(DeviceConfigPackage.class);
        ArgumentCaptor<StationContextPackage> stationContextCaptor = ArgumentCaptor.forClass(StationContextPackage.class);
        ArgumentCaptor<MediaAccessRulesPackage> mediaRulesCaptor = ArgumentCaptor.forClass(MediaAccessRulesPackage.class);
        verify(deviceConfigRepository).save(deviceConfigCaptor.capture());
        verify(stationContextRepository).save(stationContextCaptor.capture());
        verify(mediaAccessRulesRepository).save(mediaRulesCaptor.capture());
        assertThat(deviceConfigCaptor.getValue().getVersion()).isEqualTo(13);
        assertThat(deviceConfigCaptor.getValue().getMaxOfflineSeconds()).isEqualTo(60);
        assertThat(deviceConfigCaptor.getValue().getAllowOfflineValidation()).isTrue();
        assertThat(deviceConfigCaptor.getValue().getDeviceTypes()).isEqualTo("[\"QR_SCANNER_SIMULATOR\"]");
        assertThat(deviceConfigCaptor.getValue().getQrVerificationAlgorithm()).isEqualTo("HMAC_SHA256");
        assertThat(deviceConfigCaptor.getValue().getQrVerificationKey()).isEqualTo("base64url-encoded-secret");
        assertThat(deviceConfigCaptor.getValue().getQrMaxTtlSeconds()).isEqualTo(30);
        assertThat(deviceConfigCaptor.getValue().getMaxClockDriftSeconds()).isEqualTo(30);
        assertThat(deviceConfigCaptor.getValue().getHeartbeatIntervalSeconds()).isEqualTo(30);
        assertThat(stationContextCaptor.getValue().getStationName()).isEqualTo("Central");
        assertThat(stationContextCaptor.getValue().getRouteCode()).isEqualTo("METRO-001");
        assertThat(stationContextCaptor.getValue().getStationOrder()).isEqualTo(1);
        assertThat(stationContextCaptor.getValue().getDistance()).isEqualByComparingTo("0.0");
        assertThat(stationContextCaptor.getValue().getOperatorCode()).isEqualTo("HCMC-METRO");
        assertThat(mediaRulesCaptor.getValue().getVersion()).isEqualTo(5);
        assertThat(mediaRulesCaptor.getValue().getCardStatusRules()).hasSize(2);
        assertThat(mediaRulesCaptor.getValue().getCardStatusRules())
                .extracting("cardId", "status", "statusReason")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("uuid-1", "BLACKLISTED", "LOST_CARD"),
                        org.assertj.core.groups.Tuple.tuple("uuid-2", "CANCELLED", "FRAUD"));
        assertThat(deviceConfigCaptor.getValue().getPackageId()).isEqualTo("2026-06-25T00:00:00Z");
        assertThat(deviceConfigCaptor.getValue().getDeviceCode()).isEqualTo("device-1");
        assertThat(deviceConfigCaptor.getValue().getStationCode()).isEqualTo("station-1");
    }

    @Test
    void rejectsPackageForAnotherStation() throws Exception {
        DeviceInformationPackageService service = new DeviceInformationPackageService(
                new DeviceProperties("device-1", "station-1", null),
                objectMapper,
                mock(DeviceConfigPackageRepository.class),
                mock(StationContextPackageRepository.class),
                mock(MediaAccessRulesPackageRepository.class));
        DeviceInformationPackageMessage message = new DeviceInformationPackageMessage(
                1002L,
                "2026-06-25T00:00:00Z",
                objectMapper.readTree("{}"),
                objectMapper.readTree("{\"stationCode\":\"station-2\"}"),
                objectMapper.readTree("{\"cardStatusRules\":[]}"));

        assertThatThrownBy(() -> service.storePackage(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Package station code is not configured for this validation service");
    }

    @Test
    void overwritesExistingCardStatusRulesForSameStation() throws Exception {
        DeviceConfigPackageRepository deviceConfigRepository = mock(DeviceConfigPackageRepository.class);
        StationContextPackageRepository stationContextRepository = mock(StationContextPackageRepository.class);
        MediaAccessRulesPackageRepository mediaAccessRulesRepository = mock(MediaAccessRulesPackageRepository.class);
        MediaAccessRulesPackage existingRules = existingMediaRulesPackage();
        when(deviceConfigRepository.findByStationAndDeviceCode("station-1", "device-1"))
                .thenReturn(Optional.of(new DeviceConfigPackage()));
        when(stationContextRepository.findByStationCode("station-1")).thenReturn(Optional.of(new StationContextPackage()));
        when(mediaAccessRulesRepository.findByStationCode("station-1")).thenReturn(Optional.of(existingRules));
        DeviceInformationPackageService service = new DeviceInformationPackageService(
                new DeviceProperties("device-1", "station-1", null),
                objectMapper,
                deviceConfigRepository,
                stationContextRepository,
                mediaAccessRulesRepository);
        DeviceInformationPackageMessage message = new DeviceInformationPackageMessage(
                1003L,
                "2026-06-26T00:00:00Z",
                objectMapper.readTree("{}"),
                objectMapper.readTree("{\"stationCode\":\"station-1\"}"),
                objectMapper.readTree("""
                        {
                          "cardStatusRules": [
                            {
                              "cardId": "uuid-1",
                              "status": "ACTIVE",
                              "statusReason": "RESTORED"
                            }
                          ]
                        }
                        """));

        service.storePackage(message);

        assertThat(existingRules.getCardStatusRules()).hasSize(1);
        assertThat(existingRules.getCardStatusRules().get(0).getCardId()).isEqualTo("uuid-1");
        assertThat(existingRules.getCardStatusRules().get(0).getStatus()).isEqualTo("ACTIVE");
        assertThat(existingRules.getCardStatusRules().get(0).getStatusReason()).isEqualTo("RESTORED");
        verify(mediaAccessRulesRepository).save(existingRules);
    }

    private MediaAccessRulesPackage existingMediaRulesPackage() {
        MediaAccessRulesPackage packageEntity = new MediaAccessRulesPackage();
        packageEntity.setPackageId("old-package");
        packageEntity.setDeviceCode("device-1");
        packageEntity.setStationCode("station-1");
        packageEntity.setPayloadJson("{}");

        MediaAccessCardStatusRule rule = new MediaAccessCardStatusRule();
        rule.setCardId("uuid-1");
        rule.setStatus("BLACKLISTED");
        rule.setStatusReason("LOST_CARD");
        packageEntity.replaceCardStatusRules(java.util.List.of(rule));
        return packageEntity;
    }
}
