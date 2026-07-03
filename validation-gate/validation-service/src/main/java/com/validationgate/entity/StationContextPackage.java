package com.validationgate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "station_context_package",
        uniqueConstraints = @UniqueConstraint(name = "uk_station_context_station", columnNames = "station_code"))
public class StationContextPackage extends BaseEntity {

    @Column(name = "package_id", nullable = false, length = 100)
    private String packageId;

    @Column(name = "device_code", nullable = false, length = 100)
    private String deviceCode;

    @Column(name = "station_code", nullable = false, length = 100)
    private String stationCode;

    @Column(name = "station_name", length = 200)
    private String stationName;

    @Column(name = "route_code", length = 100)
    private String routeCode;

    @Column(name = "station_order")
    private Integer stationOrder;

    @Column(name = "distance", precision = 12, scale = 3)
    private BigDecimal distance;

    @Column(name = "operator_code", length = 100)
    private String operatorCode;

    @Lob
    @Column(name = "payload_json", nullable = false)
    private String payloadJson;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;
}
