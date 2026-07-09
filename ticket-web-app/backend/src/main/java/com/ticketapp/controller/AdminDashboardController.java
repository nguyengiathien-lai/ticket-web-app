package com.ticketapp.controller;

import com.ticketapp.dto.ApiResponse;
import com.ticketapp.dto.admin.AdminDashboardSummary;
import com.ticketapp.entity.Account;
import com.ticketapp.service.AdminDashboardService;
import com.ticketapp.service.AuthenticationService;
import com.ticketapp.service.AuthorizationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/dashboard")
@Tag(name = "Administration", description = "Administrative dashboard APIs")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;

    public AdminDashboardController(
            AdminDashboardService adminDashboardService,
            AuthenticationService authenticationService,
            AuthorizationService authorizationService) {
        this.adminDashboardService = adminDashboardService;
        this.authenticationService = authenticationService;
        this.authorizationService = authorizationService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<AdminDashboardSummary>> getSummary(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        requireAdmin(authorizationHeader);

        return ResponseEntity.ok(ApiResponse.success(
                adminDashboardService.getTodaySummary(),
                "Admin dashboard summary retrieved successfully"));
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
