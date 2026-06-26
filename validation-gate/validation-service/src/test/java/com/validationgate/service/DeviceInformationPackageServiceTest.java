package com.validationgate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.validationgate.config.DeviceProperties;
import com.validationgate.dto.DeviceInformationPackageMessage;
import com.validationgate.entity.DeviceConfigPackage;
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
        when(deviceConfigRepository.findByStationCode("station-1")).thenReturn(Optional.empty());
        when(stationContextRepository.findByStationCode("station-1")).thenReturn(Optional.empty());
        when(mediaAccessRulesRepository.findByStationCode("station-1")).thenReturn(Optional.empty());
        DeviceInformationPackageService service = new DeviceInformationPackageService(
                new DeviceProperties("device-1", "station-1"),
                objectMapper,
                deviceConfigRepository,
                stationContextRepository,
                mediaAccessRulesRepository);
        DeviceInformationPackageMessage message = new DeviceInformationPackageMessage(
                "2026-06-25T00:00:00Z",
                objectMapper.readTree("{\"mode\":\"online\"}"),
                objectMapper.readTree("{\"stationCode\":\"station-1\",\"name\":\"Central\"}"),
                objectMapper.readTree("{\"cardStatusRules\":[{\"cardId\":\"uuid-1\",\"status\":\"BLACKLISTED\"}]}"));

        service.storePackage(message);

        ArgumentCaptor<DeviceConfigPackage> deviceConfigCaptor = ArgumentCaptor.forClass(DeviceConfigPackage.class);
        ArgumentCaptor<StationContextPackage> stationContextCaptor = ArgumentCaptor.forClass(StationContextPackage.class);
        ArgumentCaptor<MediaAccessRulesPackage> mediaRulesCaptor = ArgumentCaptor.forClass(MediaAccessRulesPackage.class);
        verify(deviceConfigRepository).save(deviceConfigCaptor.capture());
        verify(stationContextRepository).save(stationContextCaptor.capture());
        verify(mediaAccessRulesRepository).save(mediaRulesCaptor.capture());
        assertThat(deviceConfigCaptor.getValue().getPayloadJson()).isEqualTo("{\"mode\":\"online\"}");
        assertThat(stationContextCaptor.getValue().getPayloadJson()).isEqualTo("{\"stationCode\":\"station-1\",\"name\":\"Central\"}");
        assertThat(mediaRulesCaptor.getValue().getPayloadJson()).isEqualTo("{\"cardStatusRules\":[{\"cardId\":\"uuid-1\",\"status\":\"BLACKLISTED\"}]}");
        assertThat(deviceConfigCaptor.getValue().getPackageId()).isEqualTo("2026-06-25T00:00:00Z");
        assertThat(deviceConfigCaptor.getValue().getDeviceCode()).isEqualTo("device-1");
        assertThat(deviceConfigCaptor.getValue().getStationCode()).isEqualTo("station-1");
    }

    @Test
    void rejectsPackageForAnotherStation() throws Exception {
        DeviceInformationPackageService service = new DeviceInformationPackageService(
                new DeviceProperties("device-1", "station-1"),
                objectMapper,
                mock(DeviceConfigPackageRepository.class),
                mock(StationContextPackageRepository.class),
                mock(MediaAccessRulesPackageRepository.class));
        DeviceInformationPackageMessage message = new DeviceInformationPackageMessage(
                "2026-06-25T00:00:00Z",
                objectMapper.readTree("{}"),
                objectMapper.readTree("{\"stationCode\":\"station-2\"}"),
                objectMapper.readTree("{\"cardStatusRules\":[]}"));

        assertThatThrownBy(() -> service.storePackage(message))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Package station code does not match this gate device");
    }
}
