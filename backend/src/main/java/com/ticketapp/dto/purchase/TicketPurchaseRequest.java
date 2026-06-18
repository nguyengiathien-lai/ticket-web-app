package com.ticketapp.dto.purchase;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketPurchaseRequest {

    @NotBlank
    private String userId;

    @NotBlank
    private String ticketType;

    @NotBlank
    private String paymentMethod;
}
