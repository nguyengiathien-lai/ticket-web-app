package com.ticketapp.service;

import com.ticketapp.entity.Account;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
public class AuthenticationService {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AuthorizationService authorizationService;

    /**
     * Authenticate user with email and password
     * Returns Account if successful, empty Optional if failed
     */
    public Optional<Account> authenticateByEmail(String email, String password) {
        Optional<Account> account = accountService.findActiveByEmail(email);

        if (account.isEmpty()) {
            log.warn("Authentication failed: Account not found or inactive for email: {}", email);
            return Optional.empty();
        }

        Account acc = account.get();

        // Check if email is verified
        if (!acc.getIsEmailVerified()) {
            log.warn("Authentication failed: Email not verified for: {}", email);
            return Optional.empty();
        }

        // Verify password
        if (!accountService.verifyPassword(password, acc.getPassword())) {
            log.warn("Authentication failed: Invalid password for email: {}", email);
            return Optional.empty();
        }

        log.info("Authentication successful for: {}", email);
        return account;
    }

    /**
     * Authenticate user with email and password (with password change flag check)
     */
    public Optional<Account> authenticateByEmailStrict(String email, String password) {
        Optional<Account> account = authenticateByEmail(email, password);

        if (account.isEmpty()) {
            return Optional.empty();
        }

        Account acc = account.get();

        // Check if password change is required
        if (acc.getMustChangePassword()) {
            log.info("Password change required for: {}", email);
        }

        return account;
    }

    /**
     * Validate account is active and email verified
     */
    public boolean isAccountValid(String accountId) {
        Optional<Account> account = accountService.findById(accountId);

        if (account.isEmpty()) {
            return false;
        }

        Account acc = account.get();
        return acc.getIsActive() && acc.getIsEmailVerified();
    }

    /**
     * Verify user has required permission for action
     */
    public boolean authorize(String accountId, String permissionName) {
        try {
            authorizationService.requirePermission(accountId, permissionName);
            return true;
        } catch (SecurityException e) {
            log.warn("Authorization failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Verify user has required role
     */
    public boolean authorizeRole(String accountId, String roleName) {
        try {
            authorizationService.requireRole(accountId, roleName);
            return true;
        } catch (SecurityException e) {
            log.warn("Role authorization failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get account details for authenticated user
     */
    public Optional<Account> getAuthenticatedAccount(String accountId) {
        if (!isAccountValid(accountId)) {
            return Optional.empty();
        }

        return accountService.findById(accountId);
    }

    /**
     * Check if account password change is required
     */
    public boolean isPasswordChangeRequired(String accountId) {
        return accountService.findById(accountId)
                .map(Account::getMustChangePassword)
                .orElse(false);
    }

    /**
     * Validate authentication token (basic implementation)
     * In production, this would validate JWT tokens
     */
    public boolean validateToken(String accountId) {
        return accountService.findById(accountId)
                .map(Account::getIsActive)
                .orElse(false);
    }

    /**
     * Logout (token invalidation in real implementation with JWT)
     */
    @Transactional
    public void logout(String accountId) {
        log.info("User logout: {}", accountId);
        // In production with JWT, you would add token to blacklist
        // or invalidate refresh tokens
    }
}
