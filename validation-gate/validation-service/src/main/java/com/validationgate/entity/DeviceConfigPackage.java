package com.validationgate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "device_config_package",
        uniqueConstraints = @UniqueConstraint(name = "uk_device_config_station", columnNames = "station_code"))
public class DeviceConfigPackage extends BaseEntity {

    @Column(name = "package_id", nullable = false, length = 100)
    private String packageId;

    @Column(name = "device_code", nullable = false, length = 100)
    private String deviceCode;

    @Column(name = "station_code", nullable = false, length = 100)
    private String stationCode;

    @Lob
    @Column(name = "payload_json", nullable = false)
    private String payloadJson;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;
}
