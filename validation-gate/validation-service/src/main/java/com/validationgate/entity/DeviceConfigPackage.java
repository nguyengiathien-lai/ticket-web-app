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

    @Column(name = "config_version")
    private Integer version;

    @Column(name = "max_offline_seconds")
    private Integer maxOfflineSeconds;

    @Column(name = "allow_offline_validation")
    private Boolean allowOfflineValidation;

    @Column(name = "device_types", length = 500)
    private String deviceTypes;

    @Column(name = "qr_verification_algorithm", length = 50)
    private String qrVerificationAlgorithm;

    @Column(name = "qr_verification_key", length = 500)
    private String qrVerificationKey;

    @Column(name = "qr_max_ttl_seconds")
    private Integer qrMaxTtlSeconds;

    @Column(name = "max_clock_drift_seconds")
    private Integer maxClockDriftSeconds;

    @Column(name = "heartbeat_interval_seconds")
    private Integer heartbeatIntervalSeconds;

    @Lob
    @Column(name = "payload_json", nullable = false)
    private String payloadJson;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;
}
