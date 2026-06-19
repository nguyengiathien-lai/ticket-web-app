package com.ticketapp.dto.account;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountRoleRequest {

    @NotBlank(message = "Role name is required")
    private String roleName;
}
