package com.ticketapp.dto.account;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountEmailVerificationRequest {

    @NotNull(message = "Email verification status is required")
    private Boolean emailVerified;
}
