package com.ticketapp.dto.purchase;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CardPurchaseRequest {

    @NotBlank
    private String userId;

    @NotBlank
    private String packageId;

    @NotBlank
    private String paymentMethod;

    @NotBlank
    private String deliveryAddress;
}
