package com.validationgate.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class MediaAccessCardStatusRule extends BaseEntity {

    @JsonIgnore
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
