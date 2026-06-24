package com.ticketapp.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketapp.entity.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticationServiceTest {

    private FakeAccountService accountService;
    private FakeJwtService jwtService;
    private FakeAuditService auditService;
    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        accountService = new FakeAccountService();
        jwtService = new FakeJwtService();
        auditService = new FakeAuditService();
        authenticationService = new AuthenticationService();
        ReflectionTestUtils.setField(authenticationService, "accountService", accountService);
        ReflectionTestUtils.setField(authenticationService, "authorizationService", new AuthorizationService());
        ReflectionTestUtils.setField(authenticationService, "jwtService", jwtService);
        ReflectionTestUtils.setField(authenticationService, "auditService", auditService);
    }

    @Test
    void authenticateByEmailStrictReturnsAccountWhenCredentialsAreValid() {
        Account account = account("user-1", true, true, false);
        accountService.activeAccount = Optional.of(account);
        accountService.passwordMatches = true;

        Optional<Account> result = authenticationService.authenticateByEmailStrict("user@example.com", "secret123");

        assertThat(result).contains(account);
        assertThat(auditService.action).isEqualTo(AuditService.LOGIN_SUCCESS);
        assertThat(auditService.accountId).isEqualTo("user-1");
    }

    @Test
    void authenticateByEmailStrictRejectsUnknownEmail() {
        accountService.activeAccount = Optional.empty();

        Optional<Account> result = authenticationService.authenticateByEmailStrict("missing@example.com", "secret123");

        assertThat(result).isEmpty();
        assertThat(accountService.verifyPasswordCalled).isFalse();
        assertThat(auditService.action).isEqualTo(AuditService.LOGIN_FAILURE);
        assertThat(auditService.attemptedEmail).isEqualTo("missing@example.com");
    }

    @Test
    void authenticateByEmailStrictRejectsUnverifiedAccount() {
        accountService.activeAccount = Optional.of(account("user-1", true, false, false));

        Optional<Account> result = authenticationService.authenticateByEmailStrict("user@example.com", "secret123");

        assertThat(result).isEmpty();
        assertThat(accountService.verifyPasswordCalled).isFalse();
    }

    @Test
    void authenticateByEmailStrictRejectsInvalidPassword() {
        accountService.activeAccount = Optional.of(account("user-1", true, true, false));
        accountService.passwordMatches = false;

        Optional<Account> result = authenticationService.authenticateByEmailStrict("user@example.com", "wrong");

        assertThat(result).isEmpty();
    }

    @Test
    void validateTokenReturnsTrueForValidTokenAndValidAccount() {
        jwtService.tokenValid = true;
        jwtService.accountId = "user-1";
        accountService.accountById = Optional.of(account("user-1", true, true, false));

        boolean valid = authenticationService.validateToken("token");

        assertThat(valid).isTrue();
    }

    @Test
    void validateTokenRejectsInactiveAccount() {
        jwtService.tokenValid = true;
        jwtService.accountId = "user-1";
        accountService.accountById = Optional.of(account("user-1", false, true, false));

        boolean valid = authenticationService.validateToken("token");

        assertThat(valid).isFalse();
    }

    @Test
    void logoutRevokesTokenUntilItExpires() {
        jwtService.tokenValid = true;
        jwtService.accountId = "user-1";
        jwtService.expiration = Instant.now().plusSeconds(3600).getEpochSecond();

        authenticationService.logout("token");

        assertThat(authenticationService.validateToken("token")).isFalse();
        assertThat(auditService.action).isEqualTo(AuditService.LOGOUT_SUCCESS);
        assertThat(auditService.accountId).isEqualTo("user-1");
    }

    private Account account(String id, boolean active, boolean emailVerified, boolean mustChangePassword) {
        Account account = new Account();
        account.setId(id);
        account.setEmail("user@example.com");
        account.setPassword("encoded-password");
        account.setFullName("Test User");
        account.setIsActive(active);
        account.setIsEmailVerified(emailVerified);
        account.setMustChangePassword(mustChangePassword);
        return account;
    }

    private static class FakeAccountService extends AccountService {
        private Optional<Account> activeAccount = Optional.empty();
        private Optional<Account> accountById = Optional.empty();
        private boolean passwordMatches;
        private boolean verifyPasswordCalled;

        @Override
        public Optional<Account> findActiveByEmail(String email) {
            return activeAccount;
        }

        @Override
        public Optional<Account> findById(String accountId) {
            return accountById;
        }

        @Override
        public boolean verifyPassword(String rawPassword, String encodedPassword) {
            verifyPasswordCalled = true;
            return passwordMatches;
        }
    }

    private static class FakeJwtService extends JwtService {
        private boolean tokenValid;
        private String accountId = "user-1";
        private long expiration = Instant.now().plusSeconds(3600).getEpochSecond();

        private FakeJwtService() {
            super(new ObjectMapper(), "test-secret-that-is-at-least-32-bytes", 3600);
        }

        @Override
        public boolean isTokenValid(String token) {
            return tokenValid;
        }

        @Override
        public String extractAccountId(String token) {
            return accountId;
        }

        @Override
        public long extractExpirationEpochSeconds(String token) {
            return expiration;
        }
    }

    private static class FakeAuditService extends AuditService {
        private String accountId;
        private String attemptedEmail;
        private String action;

        private FakeAuditService() {
            super(null);
        }

        @Override
        public com.ticketapp.entity.AuditLog recordAuthenticationActivity(
                String accountId,
                String attemptedEmail,
                String action,
                String details) {
            this.accountId = accountId;
            this.attemptedEmail = attemptedEmail;
            this.action = action;
            return new com.ticketapp.entity.AuditLog();
        }
    }
}
