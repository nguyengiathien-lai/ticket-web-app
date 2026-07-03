package com.ticketapp.dto.purchase;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardPurchaseResponse {

    private IssuedCard card;
    private IssuedTicket ticket;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssuedCard {
        private String id;
        private String cardUid;
        private String status;
        private String type;
        private Boolean supportsMetro;
        private Boolean supportsBus;
        private String issuedAtStationId;
        private String linkedUserId;
        private LocalDateTime activatedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssuedTicket {
        private String ticketId;
        private String type;
        private String mode;
        private String scope;
        private String routeId;
        private String status;
        private String cardId;
        private String userId;
        private String fromStationCode;
        private String toStationCode;
        private BigDecimal price;
        private String fareRuleId;
        private String discountId;
        private LocalDate validFrom;
        private LocalDate validTo;
        private LocalDateTime purchasedAt;
    }
}
