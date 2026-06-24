package com.ticketapp.dto.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class TicketTypeSyncRequest {

    @NotBlank(message = "External ticket type id is required")
    private String externalTicketTypeId;

    @NotBlank(message = "Ticket type code is required")
    private String code;

    @NotBlank(message = "Ticket type name is required")
    private String name;

    private Integer durationDays;

    @NotNull(message = "Price is required")
    @PositiveOrZero(message = "Price must be zero or greater")
    private BigDecimal price;

    @NotBlank(message = "Currency is required")
    private String currency;

    private String description;

    private Boolean active = true;

    private LocalDateTime expiresAt;
}
