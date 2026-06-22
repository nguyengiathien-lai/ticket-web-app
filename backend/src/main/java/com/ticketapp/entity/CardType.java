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
public class CardType {

    private String externalCardTypeId;

    private String code;

    private String name;

    private Integer durationDays;

    private BigDecimal price;

    private String currency;

    private String description;

    private Boolean active = true;

    private LocalDateTime cachedAt;

    private LocalDateTime expiresAt;
}
