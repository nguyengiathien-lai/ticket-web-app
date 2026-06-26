package com.validationgate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.device")
public record DeviceProperties(String code, String stationCode) {

    public DeviceProperties {
        code = hasText(code) ? code.trim() : "gate-device-1";
        stationCode = hasText(stationCode) ? stationCode.trim() : "station-1";
    }

    public String packageQueueName() {
        return "device." + stationCode;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
