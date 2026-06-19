package com.ticketapp.dto.purchase;

import com.ticketapp.entity.CardType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CardTypeResponse {

    private String externalCardTypeId;
    private String packageId;
    private String code;
    private String name;
    private Integer durationDays;
    private BigDecimal price;
    private String currency;
    private String description;
    private Boolean active;
    private LocalDateTime cachedAt;
    private LocalDateTime expiresAt;

    public static CardTypeResponse from(CardType cardType) {
        return CardTypeResponse.builder()
                .externalCardTypeId(cardType.getExternalCardTypeId())
                .packageId(cardType.getCode())
                .code(cardType.getCode())
                .name(cardType.getName())
                .durationDays(cardType.getDurationDays())
                .price(cardType.getPrice())
                .currency(cardType.getCurrency())
                .description(cardType.getDescription())
                .active(cardType.getActive())
                .cachedAt(cardType.getCachedAt())
                .expiresAt(cardType.getExpiresAt())
                .build();
    }
}
