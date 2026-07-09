package com.ticketapp.dto.purchase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CardPurchaseRequest {

    @Valid
    @NotNull
    private CardDetails card;

    @Valid
    @NotNull
    private TicketDetails ticket;

    @NotBlank
    private String deliveryAddress;

    @Getter
    @Setter
    public static class CardDetails {
        private String cardUid;
        private String userId;
        private Boolean supportsMetro;
        private Boolean supportsBus;
    }

    @Getter
    @Setter
    public static class TicketDetails {
        private String userId;
        private String mode;
        private String scope;
        private String routeId;
        private String passengerType;
        private LocalDate validFrom;
        private String durationType;
        private Integer durationMonths;
    }
}
