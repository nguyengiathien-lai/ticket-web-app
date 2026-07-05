package com.ticketapp.controller;

import com.ticketapp.dto.ApiResponse;
import com.ticketapp.dto.admin.AdminLoginHistoryItem;
import com.ticketapp.entity.Account;
import com.ticketapp.service.AdminLoginHistoryService;
import com.ticketapp.service.AuthenticationService;
import com.ticketapp.service.AuthorizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/login-history")
public class AdminLoginHistoryController {

    private final AdminLoginHistoryService adminLoginHistoryService;
    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;

    public AdminLoginHistoryController(
            AdminLoginHistoryService adminLoginHistoryService,
            AuthenticationService authenticationService,
            AuthorizationService authorizationService) {
        this.adminLoginHistoryService = adminLoginHistoryService;
        this.authenticationService = authenticationService;
        this.authorizationService = authorizationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminLoginHistoryItem>>> getLoginHistory(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        requireAdmin(authorizationHeader);

        return ResponseEntity.ok(ApiResponse.success(
                adminLoginHistoryService.getRecentLoginHistory(),
                "Login history retrieved successfully"));
    }

    private Account requireAdmin(String authorizationHeader) {
        Account account = authenticationService.getAuthenticatedAccountByToken(extractBearerToken(authorizationHeader))
                .orElseThrow(() -> new SecurityException("Valid authentication token is required"));

        authorizationService.requireRole(account.getId(), "APP_ADMIN");
        return account;
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            return "";
        }

        if (authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authorizationHeader.substring(7).trim();
        }

        return authorizationHeader.trim();
    }
}
