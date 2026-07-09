package com.validationgate.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DevicePropertiesTest {

    private static final String REGISTRATIONS = """
            HN_2A_01:DEV_HN2A01_ENT_01,HN_2A_01:DEV_HN2A01_EXT_01,\
            HN_2A_02:DEV_HN2A02_ENT_01,HN_2A_02:DEV_HN2A02_EXT_01,\
            HN_2A_12:DEV_HN2A12_ENT_01,HN_2A_12:DEV_HN2A12_EXT_01,\
            BUS32_01:DEV_BUS3201_BOTH_01,BRT01_10:DEV_BRT0110_BOTH_01
            """;

    @Test
    void parsesAllEightStationDevicePairs() {
        DeviceProperties properties = new DeviceProperties(null, null, REGISTRATIONS);

        assertThat(properties.devices()).hasSize(8);
        assertThat(properties.supports("HN_2A_01", "DEV_HN2A01_ENT_01")).isTrue();
        assertThat(properties.supports("HN_2A_01", "DEV_HN2A01_EXT_01")).isTrue();
        assertThat(properties.supports("BRT01_10", "DEV_BRT0110_BOTH_01")).isTrue();
    }

    @Test
    void createsOneQueuePerDistinctDevice() {
        DeviceProperties properties = new DeviceProperties(null, null, REGISTRATIONS);

        assertThat(properties.packageQueueNames()).containsExactlyInAnyOrder(
                "device.DEV_HN2A01_ENT_01",
                "device.DEV_HN2A01_EXT_01",
                "device.DEV_HN2A02_ENT_01",
                "device.DEV_HN2A02_EXT_01",
                "device.DEV_HN2A12_ENT_01",
                "device.DEV_HN2A12_EXT_01",
                "device.DEV_BUS3201_BOTH_01",
                "device.DEV_BRT0110_BOTH_01");
    }

    @Test
    void rejectsUnknownStationDeviceCombinations() {
        DeviceProperties properties = new DeviceProperties(null, null, REGISTRATIONS);

        assertThat(properties.supports("HN_2A_01", "DEV_HN2A02_ENT_01")).isFalse();
        assertThat(properties.supportsStation("unknown")).isFalse();
    }

    @Test
    void keepsLegacySingleDeviceConfigurationAsFallback() {
        DeviceProperties properties = new DeviceProperties("legacy-device", "legacy-station", null);

        assertThat(properties.packageQueueNames()).containsExactly("device.legacy-device");
        assertThat(properties.supports("legacy-station", "legacy-device")).isTrue();
    }

    @Test
    void rejectsMalformedRegistration() {
        DeviceProperties properties = new DeviceProperties(null, null, "missing-separator");

        assertThatThrownBy(properties::devices)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expected stationCode:deviceCode");
    }

    @Test
    void bindsThroughSpringBootConfigurationProperties() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "app.device.code", "fallback-device",
                "app.device.station-code", "fallback-station",
                "app.device.registrations", REGISTRATIONS));

        DeviceProperties properties = new Binder(source)
                .bind("app.device", DeviceProperties.class)
                .orElseThrow(() -> new AssertionError("Device properties did not bind"));

        assertThat(properties.devices()).hasSize(8);
        assertThat(properties.packageQueueNames()).hasSize(8);
    }
}
