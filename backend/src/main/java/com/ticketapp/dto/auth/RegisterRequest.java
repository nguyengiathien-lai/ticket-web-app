package com.ticketapp.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 50, message = "Email must be at most 50 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must be at most 100 characters")
    private String fullName;

    @Size(max = 10, message = "Phone number must be at most 10 characters")
    private String phoneNumber;

    @NotBlank(message = "Personal id is required")
    @Size(max = 12, message = "Personal id must be at most 12 characters")
    private String personalId;

    @Size(max = 200, message = "Address must be at most 200 characters")
    private String address;
}
