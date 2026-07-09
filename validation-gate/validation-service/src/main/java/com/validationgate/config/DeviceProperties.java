package com.validationgate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ConfigurationProperties(prefix = "app.device")
public record DeviceProperties(String code, String stationCode, String registrations) {

    public DeviceProperties {
        code = hasText(code) ? code.trim() : "gate-device-1";
        stationCode = hasText(stationCode) ? stationCode.trim() : "station-1";
    }

    /**
     * Configured as comma-separated {@code stationCode:deviceCode} pairs.
     * The legacy single station/device properties remain a fallback.
     */
    public List<DeviceRegistration> devices() {
        if (!hasText(registrations)) {
            return List.of(new DeviceRegistration(stationCode, code));
        }

        List<DeviceRegistration> parsed = Arrays.stream(registrations.split(","))
                .map(String::trim)
                .filter(DeviceProperties::hasText)
                .map(DeviceRegistration::parse)
                .distinct()
                .toList();
        return parsed.isEmpty() ? List.of(new DeviceRegistration(stationCode, code)) : parsed;
    }

    public boolean supports(String station, String device) {
        return devices().stream().anyMatch(registration ->
                registration.stationCode().equals(station) && registration.deviceCode().equals(device));
    }

    public boolean supportsStation(String station) {
        return devices().stream().anyMatch(registration -> registration.stationCode().equals(station));
    }

    public String deviceCodeForStation(String station) {
        return devices().stream()
                .filter(registration -> registration.stationCode().equals(station))
                .map(DeviceRegistration::deviceCode)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No device is configured for station " + station));
    }

    public Set<String> stationCodes() {
        LinkedHashSet<String> stations = new LinkedHashSet<>();
        devices().forEach(registration -> stations.add(registration.stationCode()));
        return Set.copyOf(stations);
    }

    public String[] packageQueueNames() {
        return devices().stream()
                .map(DeviceRegistration::deviceCode)
                .distinct()
                .map(device -> "device." + device)
                .toArray(String[]::new);
    }

    public String packageQueueName() {
        return "device." + code;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record DeviceRegistration(String stationCode, String deviceCode) {
        public DeviceRegistration {
            if (!hasText(stationCode) || !hasText(deviceCode)) {
                throw new IllegalArgumentException("Device registration requires stationCode:deviceCode");
            }
            stationCode = stationCode.trim();
            deviceCode = deviceCode.trim();
        }

        static DeviceRegistration parse(String value) {
            String[] parts = value.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException(
                        "Invalid device registration '" + value + "'; expected stationCode:deviceCode");
            }
            return new DeviceRegistration(parts[0], parts[1]);
        }
    }
}
