package com.ticketapp.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private String tokenType;
    private Boolean mustChangePassword;
    private AccountResponse account;
}
