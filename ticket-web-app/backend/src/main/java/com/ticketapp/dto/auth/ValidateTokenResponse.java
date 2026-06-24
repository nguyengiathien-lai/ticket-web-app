package com.ticketapp.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ValidateTokenResponse {
    private boolean valid;
    private AccountResponse account;

    public ValidateTokenResponse(boolean valid) {
        this.valid = valid;
    }
}
