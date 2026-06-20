package com.ticketapp.controller;

import com.ticketapp.dto.ApiResponse;
import com.ticketapp.dto.account.AccountEmailVerificationRequest;
import com.ticketapp.dto.account.AccountRoleRequest;
import com.ticketapp.dto.account.AccountStatusRequest;
import com.ticketapp.dto.account.AdminResetPasswordRequest;
import com.ticketapp.dto.auth.AccountResponse;
import com.ticketapp.dto.card.PhysicalCardResponse;
import com.ticketapp.dto.ticket.TicketResponse;
import com.ticketapp.entity.Account;
import com.ticketapp.service.AccountService;
import com.ticketapp.service.AuthenticationService;
import com.ticketapp.service.AuthorizationService;
import com.ticketapp.service.PhysicalCardService;
import com.ticketapp.service.TicketRequestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AuthenticationService authenticationService;
    private final AuthorizationService authorizationService;
    private final TicketRequestService ticketRequestService;
    private final PhysicalCardService physicalCardService;

    public AccountController(
            AccountService accountService,
            AuthenticationService authenticationService,
            AuthorizationService authorizationService,
            TicketRequestService ticketRequestService,
            PhysicalCardService physicalCardService) {
        this.accountService = accountService;
        this.authenticationService = authenticationService;
        this.authorizationService = authorizationService;
        this.ticketRequestService = ticketRequestService;
        this.physicalCardService = physicalCardService;
    }

    @GetMapping("/{accountId}/tickets")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> getPastTickets(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @PathVariable String accountId) {
        requireSelfOrAdmin(authorizationHeader, accountId);

        return ResponseEntity.ok(ApiResponse.success(
                ticketRequestService.getCachedTicketsForPassenger(accountId),
                "Past tickets retrieved successfully"));
    }

    @GetMapping("/{accountId}/cards")
    public ResponseEntity<ApiResponse<List<PhysicalCardResponse>>> getPastCards(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @PathVariable String accountId) {
        requireSelfOrAdmin(authorizationHeader, accountId);

        return ResponseEntity.ok(ApiResponse.success(
                physicalCardService.getCardsForPassenger(accountId),
                "Past cards retrieved successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getAllAccounts(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader) {
        requireAdmin(authorizationHeader);

        List<AccountResponse> accounts = accountService.getAllAccounts()
                .stream()
                .map(AccountResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(accounts, "Accounts retrieved successfully"));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountById(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @PathVariable String accountId) {
        requireAdmin(authorizationHeader);

        Account account = accountService.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        return ResponseEntity.ok(ApiResponse.success(AccountResponse.from(account), "Account retrieved successfully"));
    }

    @PutMapping("/{accountId}/activate")
    public ResponseEntity<ApiResponse<AccountResponse>> activateAccount(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @PathVariable String accountId) {
        requireAdmin(authorizationHeader);

        Account account = accountService.activateAccount(accountId);
        return ResponseEntity.ok(ApiResponse.success(AccountResponse.from(account), "Account activated successfully"));
    }

    @PutMapping("/{accountId}/deactivate")
    public ResponseEntity<ApiResponse<AccountResponse>> deactivateAccount(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @PathVariable String accountId) {
        Account admin = requireAdmin(authorizationHeader);
        preventSelfManagement(admin, accountId, "deactivate your own admin account");

        Account account = accountService.deactivateAccount(accountId);
        return ResponseEntity.ok(ApiResponse.success(AccountResponse.from(account), "Account inactivated successfully"));
    }

    // @PutMapping("/{accountId}/status")
    // public ResponseEntity<ApiResponse<AccountResponse>> updateAccountStatus(
    //         @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
    //         @PathVariable String accountId,
    //         @Valid @RequestBody AccountStatusRequest request) {
    //     Account admin = requireAdmin(authorizationHeader);
    //     if (!request.getActive()) {
    //         preventSelfManagement(admin, accountId, "inactivate your own admin account");
    //     }

    //     Account account = accountService.updateStatus(accountId, request.getActive());
    //     return ResponseEntity.ok(ApiResponse.success(AccountResponse.from(account), "Account status updated successfully"));
    // }

    @PutMapping("/{accountId}/email-verification")
    public ResponseEntity<ApiResponse<AccountResponse>> updateEmailVerificationStatus(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @PathVariable String accountId,
            @Valid @RequestBody AccountEmailVerificationRequest request) {
        requireAdmin(authorizationHeader);

        Account account = accountService.updateEmailVerificationStatus(accountId, request.getEmailVerified());
        return ResponseEntity.ok(ApiResponse.success(
                AccountResponse.from(account),
                "Account email verification status updated successfully"));
    }

    @PostMapping("/{accountId}/roles")
    public ResponseEntity<ApiResponse<AccountResponse>> assignRole(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @PathVariable String accountId,
            @Valid @RequestBody AccountRoleRequest request) {
        requireAdmin(authorizationHeader);

        Account account = accountService.assignRole(accountId, request.getRoleName());
        return ResponseEntity.ok(ApiResponse.success(AccountResponse.from(account), "Role assigned successfully"));
    }

    @DeleteMapping("/{accountId}/roles")
    public ResponseEntity<ApiResponse<AccountResponse>> removeRole(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @PathVariable String accountId,
            @Valid @RequestBody AccountRoleRequest request) {
        Account admin = requireAdmin(authorizationHeader);
        if ("APP_ADMIN".equals(request.getRoleName())) {
            preventSelfManagement(admin, accountId, "remove your own admin role");
        }

        Account account = accountService.removeRole(accountId, request.getRoleName());
        return ResponseEntity.ok(ApiResponse.success(AccountResponse.from(account), "Role removed successfully"));
    }

    @PutMapping("/{accountId}/password/reset")
    public ResponseEntity<ApiResponse<AccountResponse>> resetPassword(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @PathVariable String accountId,
            @Valid @RequestBody AdminResetPasswordRequest request) {
        requireAdmin(authorizationHeader);

        Account account = accountService.resetPassword(accountId, request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(AccountResponse.from(account), "Password reset successfully"));
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(
            @RequestHeader(name = "Authorization", required = false) String authorizationHeader,
            @PathVariable String accountId) {
        Account admin = requireAdmin(authorizationHeader);
        preventSelfManagement(admin, accountId, "delete your own admin account");

        accountService.deleteAccount(accountId);
        return ResponseEntity.ok(ApiResponse.success(null, "Account deleted successfully"));
    }

    private Account requireAdmin(String authorizationHeader) {
        return authenticationService.getAuthenticatedAccountByToken(extractBearerToken(authorizationHeader))
                .map(account -> {
                    authorizationService.requireAdmin(account.getId());
                    return account;
                })
                .orElseThrow(() -> new SecurityException("Valid admin token is required"));
    }

    private Account requireSelfOrAdmin(String authorizationHeader, String targetAccountId) {
        Account authenticatedAccount = authenticationService
                .getAuthenticatedAccountByToken(extractBearerToken(authorizationHeader))
                .orElseThrow(() -> new SecurityException("Valid authentication token is required"));

        if (!authenticatedAccount.getId().equals(targetAccountId)) {
            authorizationService.requireAdmin(authenticatedAccount.getId());
            accountService.findById(targetAccountId)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        }

        return authenticatedAccount;
    }

    private void preventSelfManagement(Account admin, String targetAccountId, String action) {
        if (admin.getId().equals(targetAccountId)) {
            throw new IllegalArgumentException("You cannot " + action);
        }
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
