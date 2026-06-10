package com.ticketapp.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogoutRequest {
    @NotBlank(message = "Account id is required")
    private String accountId;
}
