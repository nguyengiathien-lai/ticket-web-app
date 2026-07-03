package com.validationgate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "media_access_rules_package",
        uniqueConstraints = @UniqueConstraint(name = "uk_media_rules_station", columnNames = "station_code"))
public class MediaAccessRulesPackage extends BaseEntity {

    @Column(name = "package_id", nullable = false, length = 100)
    private String packageId;

    @Column(name = "device_code", nullable = false, length = 100)
    private String deviceCode;

    @Column(name = "station_code", nullable = false, length = 100)
    private String stationCode;

    @Column(name = "rules_version")
    private Integer version;

    @Lob
    @Column(name = "payload_json", nullable = false)
    private String payloadJson;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    @OneToMany(mappedBy = "packageEntity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MediaAccessCardStatusRule> cardStatusRules = new ArrayList<>();

    public void replaceCardStatusRules(List<MediaAccessCardStatusRule> rules) {
        cardStatusRules.clear();
        rules.forEach(rule -> {
            rule.setPackageEntity(this);
            cardStatusRules.add(rule);
        });
    }
}
