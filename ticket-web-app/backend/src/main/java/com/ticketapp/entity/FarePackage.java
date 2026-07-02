package com.ticketapp.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FarePackage {

    private String code;

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

    private Boolean active = true;

    private LocalDateTime cachedAt;

    private LocalDateTime expiresAt;
}
