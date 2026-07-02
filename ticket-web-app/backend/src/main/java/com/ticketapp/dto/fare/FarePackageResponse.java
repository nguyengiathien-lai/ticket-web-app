package com.ticketapp.dto.fare;

import com.ticketapp.entity.FarePackage;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class FarePackageResponse {

    private String code;
    private String packageId;
    private String name;
    private String kind;
    private String mode;
    private String scope;
    private String durationType;
    private Integer durationDays;
    private Integer durationMonths;
    private BigDecimal price;
    private String currency;
    private String description;
    private Boolean active;
    private LocalDateTime cachedAt;
    private LocalDateTime expiresAt;

    public static FarePackageResponse from(FarePackage farePackage) {
        return FarePackageResponse.builder()
                .code(farePackage.getCode())
                .packageId(farePackage.getCode())
                .name(farePackage.getName())
                .kind(farePackage.getKind())
                .mode(farePackage.getMode())
                .scope(farePackage.getScope())
                .durationType(farePackage.getDurationType())
                .durationDays(farePackage.getDurationDays())
                .durationMonths(farePackage.getDurationMonths())
                .price(farePackage.getPrice())
                .currency(farePackage.getCurrency())
                .description(farePackage.getDescription())
                .active(farePackage.getActive())
                .cachedAt(farePackage.getCachedAt())
                .expiresAt(farePackage.getExpiresAt())
                .build();
    }
}
