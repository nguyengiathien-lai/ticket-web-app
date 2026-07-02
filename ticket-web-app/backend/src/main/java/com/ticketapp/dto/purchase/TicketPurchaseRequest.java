package com.ticketapp.dto.purchase;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketPurchaseRequest {

    @NotBlank
    private String userId;

    @NotBlank
    @JsonAlias("ticketType")
    private String packageId;

    @NotBlank
    private String paymentMethod;
}
