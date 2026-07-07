package com.ticketapp.dto.purchase;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TicketPurchaseRequest {

    @NotBlank
    private String userId;

    // @JsonAlias({"ticketType", "ticketTypeCode"})
    // private String packageId;

    private String paymentMethod;

    @JsonAlias({"type", "ticketKind"})
    private String ticketType;

    private String fromStationId;

    private String toStationId;

    private String mode;

    private String scope;

    private String routeId;

    private String passengerType;

    private LocalDate validFrom;

    private String durationType;

    private Integer durationMonths;
}
