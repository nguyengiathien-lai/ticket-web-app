package com.ticketapp.dto.auth;

import com.ticketapp.entity.Account;
import com.ticketapp.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
public class AccountResponse {
    private String id;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String personalId;
    private String address;
    private Boolean isActive;
    private Boolean isEmailVerified;
    private Boolean mustChangePassword;
    private Set<String> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AccountResponse from(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .email(account.getEmail())
                .fullName(account.getFullName())
                .phoneNumber(account.getPhoneNumber())
                .personalId(account.getPersonalId())
                .address(account.getAddress())
                .isActive(account.getIsActive())
                .isEmailVerified(account.getIsEmailVerified())
                .mustChangePassword(account.getMustChangePassword())
                .roles(account.getRoles().stream()
                        .map(Role::getName)
                        .collect(Collectors.toSet()))
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
