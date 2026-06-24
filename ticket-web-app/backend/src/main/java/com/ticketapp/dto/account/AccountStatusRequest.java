package com.ticketapp.dto.account;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountStatusRequest {

    @NotNull(message = "Active status is required")
    private Boolean active;
}
