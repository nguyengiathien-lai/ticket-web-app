package com.ticketapp.dto.card;

import com.ticketapp.entity.PhysicalCard;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PhysicalCardResponse {

    private String externalCardId;
    private String passengerAccountId;
    private String cardUid;
    private String maskedCardNumber;
    private String status;
    private LocalDateTime issuedAt;
    private LocalDateTime expiredAt;

    public static PhysicalCardResponse from(PhysicalCard card) {
        return PhysicalCardResponse.builder()
                .externalCardId(card.getExternalCardId())
                .passengerAccountId(card.getPassengerAccountId())
                .cardUid(card.getCardUid())
                .maskedCardNumber(card.getMaskedCardNumber())
                .status(card.getStatus())
                .issuedAt(card.getIssuedAt())
                .expiredAt(card.getExpiredAt())
                .build();
    }
}
