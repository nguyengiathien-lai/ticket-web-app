package com.ticketapp.dto.account;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ProfileUpdateRequest {
    private String fullName;
    private String phoneNumber;
    private String personalId;
    private String address;
    private LocalDate dateOfBirth;
    private String gender;
}
