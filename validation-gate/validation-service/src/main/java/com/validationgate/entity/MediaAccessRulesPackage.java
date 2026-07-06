package com.validationgate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
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
        Map<String, MediaAccessCardStatusRule> existingByCardId = new LinkedHashMap<>();
        cardStatusRules.forEach(rule -> existingByCardId.put(rule.getCardId(), rule));

        Map<String, MediaAccessCardStatusRule> replacementByCardId = new LinkedHashMap<>();
        rules.forEach(rule -> {
            MediaAccessCardStatusRule target = existingByCardId.getOrDefault(
                    rule.getCardId(),
                    new MediaAccessCardStatusRule());
            target.setPackageEntity(this);
            target.setCardId(rule.getCardId());
            target.setStatus(rule.getStatus());
            target.setStatusReason(rule.getStatusReason());
            target.setRuleUpdatedAt(rule.getRuleUpdatedAt());
            replacementByCardId.put(rule.getCardId(), target);
        });

        cardStatusRules.removeIf(rule -> !replacementByCardId.containsKey(rule.getCardId()));
        replacementByCardId.values().forEach(rule -> {
            if (!cardStatusRules.contains(rule)) {
                cardStatusRules.add(rule);
            }
        });
    }
}
