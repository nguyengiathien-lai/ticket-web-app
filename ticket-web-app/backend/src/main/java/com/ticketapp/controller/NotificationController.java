package com.ticketapp.controller;

import com.ticketapp.dto.ApiResponse;
import com.ticketapp.dto.order.OrderNotificationResponse;
import com.ticketapp.entity.Account;
import com.ticketapp.service.AuthenticationService;
import com.ticketapp.service.OrderNotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "Passenger purchase notifications")
public class NotificationController {

    private final AuthenticationService authenticationService;
    private final OrderNotificationService notificationService;

    public NotificationController(
            AuthenticationService authenticationService,
            OrderNotificationService notificationService) {
        this.authenticationService = authenticationService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderNotificationResponse>>> getNotifications(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        Account account = authenticationService
                .getAuthenticatedAccountByToken(extractToken(authorizationHeader))
                .orElseThrow(() -> new SecurityException("Valid authentication token is required"));
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.getForPassenger(account.getId()),
                "Notifications retrieved successfully"));
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) return "";
        return authorizationHeader.regionMatches(true, 0, "Bearer ", 0, 7)
                ? authorizationHeader.substring(7).trim()
                : authorizationHeader.trim();
    }
}
