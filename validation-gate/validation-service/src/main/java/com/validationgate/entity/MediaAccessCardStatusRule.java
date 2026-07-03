package com.validationgate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "media_access_card_status_rule",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_media_card_status_rule_package_card",
                columnNames = {"package_id_fk", "card_id"}))
public class MediaAccessCardStatusRule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_id_fk", nullable = false)
    private MediaAccessRulesPackage packageEntity;

    @Column(name = "card_id", nullable = false, length = 100)
    private String cardId;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "status_reason", length = 100)
    private String statusReason;

    @Column(name = "rule_updated_at")
    private LocalDateTime ruleUpdatedAt;
}
