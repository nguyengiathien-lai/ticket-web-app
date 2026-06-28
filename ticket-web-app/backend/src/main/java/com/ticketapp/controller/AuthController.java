package com.ticketapp.controller;

import com.ticketapp.dto.ApiResponse;
import com.ticketapp.dto.auth.AccountResponse;
import com.ticketapp.dto.auth.LoginRequest;
import com.ticketapp.dto.auth.LoginResponse;
import com.ticketapp.dto.auth.LogoutRequest;
import com.ticketapp.dto.auth.RegisterRequest;
import com.ticketapp.dto.auth.ResendEmailOtpRequest;
import com.ticketapp.dto.auth.TokenValidationRequest;
import com.ticketapp.dto.auth.UpdatePasswordRequest;
import com.ticketapp.dto.auth.ValidateTokenResponse;
import com.ticketapp.dto.auth.VerifyEmailRequest;
import com.ticketapp.entity.Account;
import com.ticketapp.service.AccountService;
import com.ticketapp.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final AccountService accountService;

    public AuthController(AuthenticationService authenticationService, AccountService accountService) {
        this.authenticationService = authenticationService;
        this.accountService = accountService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AccountResponse>> register(@Valid @RequestBody RegisterRequest request) {
        Account account = accountService.registerAccount(
                request.getEmail(),
                request.getPassword(),
                request.getFirstName(),
                request.getLastName()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        AccountResponse.from(account),
                        "Account registered successfully. Check your email for the verification code."));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        Optional<Account> account = authenticationService.authenticateByEmailStrict(
                request.getEmail(),
                request.getPassword()
        );

        if (account.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid credentials or account is not ready for login"));
        }

        Account authenticatedAccount = account.get();
        String token = authenticationService.generateToken(authenticatedAccount);
        LoginResponse response = LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresAt(authenticationService.getTokenExpirationEpochSeconds(token))
                .mustChangePassword(authenticatedAccount.getMustChangePassword())
                .account(AccountResponse.from(authenticatedAccount))
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/validate-token")
    public ResponseEntity<ApiResponse<ValidateTokenResponse>> validateToken(
            @Valid @RequestBody TokenValidationRequest request) {
        Optional<Account> account = authenticationService.getAuthenticatedAccountByToken(request.getToken());

        if (account.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<ValidateTokenResponse>builder()
                            .success(false)
                            .message("Token is invalid")
                            .data(new ValidateTokenResponse(false))
                            .timestamp(java.time.LocalDateTime.now())
                            .build());
        }

        return ResponseEntity.ok(ApiResponse.success(
                new ValidateTokenResponse(true, AccountResponse.from(account.get())),
                "Token is valid"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody LogoutRequest request) {
        if (!authenticationService.validateToken(request.getToken())) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid token"));
        }

        authenticationService.logout(request.getToken());
        return ResponseEntity.ok(ApiResponse.success(null, "Logout successful"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AccountResponse>> getAuthenticatedAccount(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        return authenticationService.getAuthenticatedAccountByToken(extractBearerToken(authorizationHeader))
                .map(account -> ResponseEntity.ok(
                        ApiResponse.success(AccountResponse.from(account), "Account retrieved successfully")))
                .orElseGet(() -> ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Account is not authenticated or is inactive")));
    }

//     @GetMapping("/me/{token}")
//     public ResponseEntity<ApiResponse<AccountResponse>> getAuthenticatedAccountByTokenPath(@PathVariable String token) {
//         return authenticationService.getAuthenticatedAccountByToken(token)
//                 .map(account -> ResponseEntity.ok(
//                         ApiResponse.success(AccountResponse.from(account), "Account retrieved successfully")))
//                 .orElseGet(() -> ResponseEntity
//                         .status(HttpStatus.UNAUTHORIZED)
//                         .body(ApiResponse.error("Account is not authenticated or is inactive")));
//     }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<AccountResponse>> updatePassword(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @Valid @RequestBody UpdatePasswordRequest request) {
        Optional<Account> authenticatedAccount = authenticationService
                .getAuthenticatedAccountByToken(extractBearerToken(authorizationHeader));

        if (authenticatedAccount.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Valid authentication token is required"));
        }

        Account account = accountService.updatePassword(
                authenticatedAccount.get().getId(),
                request.getOldPassword(),
                request.getNewPassword()
        );

        return ResponseEntity.ok(ApiResponse.success(AccountResponse.from(account), "Password updated successfully"));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<AccountResponse>> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        Account account = accountService.verifyEmailWithOtp(request.getEmail(), request.getCode());
        return ResponseEntity.ok(ApiResponse.success(AccountResponse.from(account), "Email verified successfully"));
    }

    @PostMapping("/resend-email-otp")
    public ResponseEntity<ApiResponse<Void>> resendEmailOtp(@Valid @RequestBody ResendEmailOtpRequest request) {
        accountService.resendEmailVerificationOtp(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(null, "Verification code sent"));
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
