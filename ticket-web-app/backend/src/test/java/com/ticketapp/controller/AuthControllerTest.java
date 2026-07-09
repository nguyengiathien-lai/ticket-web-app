package com.ticketapp.controller;

import com.ticketapp.dto.ApiResponse;
import com.ticketapp.dto.auth.AccountResponse;
import com.ticketapp.dto.auth.LoginRequest;
import com.ticketapp.dto.auth.LoginResponse;
import com.ticketapp.dto.auth.LogoutRequest;
import com.ticketapp.dto.auth.TokenValidationRequest;
import com.ticketapp.dto.auth.UpdatePasswordRequest;
import com.ticketapp.dto.auth.ValidateTokenResponse;
import com.ticketapp.entity.Account;
import com.ticketapp.service.AccountService;
import com.ticketapp.service.AuthenticationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AuthControllerTest {

    @Test
    void loginReturnsTokenWhenCredentialsAreValid() {
        FakeAuthenticationService authenticationService = new FakeAuthenticationService();
        AuthController controller = new AuthController(authenticationService, new AccountService());
        Account account = account();
        authenticationService.authenticatedAccount = Optional.of(account);
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("secret123");

        ResponseEntity<ApiResponse<LoginResponse>> response = controller.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getData().getToken()).isEqualTo("jwt-token");
        assertThat(response.getBody().getData().getTokenType()).isEqualTo("Bearer");
        assertThat(response.getBody().getData().getAccount().getId()).isEqualTo("user-1");
    }

    @Test
    void loginReturnsUnauthorizedWhenCredentialsAreInvalid() {
        FakeAuthenticationService authenticationService = new FakeAuthenticationService();
        AuthController controller = new AuthController(authenticationService, new AccountService());
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("wrong");

        ResponseEntity<ApiResponse<LoginResponse>> response = controller.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage())
                .isEqualTo("Thông tin đăng nhập không đúng hoặc tài khoản chưa sẵn sàng để đăng nhập");
    }

    @Test
    void validateTokenReturnsInvalidPayloadForInvalidToken() {
        FakeAuthenticationService authenticationService = new FakeAuthenticationService();
        AuthController controller = new AuthController(authenticationService, new AccountService());
        TokenValidationRequest request = new TokenValidationRequest();
        request.setToken("bad-token");

        ResponseEntity<ApiResponse<ValidateTokenResponse>> response = controller.validateToken(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getData().isValid()).isFalse();
    }

    @Test
    void logoutRevokesValidToken() {
        FakeAuthenticationService authenticationService = new FakeAuthenticationService();
        authenticationService.tokenValid = true;
        AuthController controller = new AuthController(authenticationService, new AccountService());
        LogoutRequest request = new LogoutRequest();
        request.setToken("jwt-token");

        ResponseEntity<ApiResponse<Void>> response = controller.logout(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(authenticationService.loggedOutToken).isEqualTo("jwt-token");
    }

    @Test
    void updatePasswordUsesAuthenticatedAccountFromAuthorizationHeader() {
        FakeAuthenticationService authenticationService = new FakeAuthenticationService();
        FakeAccountService accountService = new FakeAccountService();
        AuthController controller = new AuthController(authenticationService, accountService);
        authenticationService.accountByToken = Optional.of(account());
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("old-secret");
        request.setNewPassword("new-secret");

        ResponseEntity<ApiResponse<AccountResponse>> response = controller.updatePassword("Bearer jwt-token", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(accountService.updatedAccountId).isEqualTo("user-1");
        assertThat(accountService.oldPassword).isEqualTo("old-secret");
        assertThat(accountService.newPassword).isEqualTo("new-secret");
    }

    @Test
    void updatePasswordReturnsUnauthorizedWithoutValidAuthorizationHeader() {
        FakeAuthenticationService authenticationService = new FakeAuthenticationService();
        FakeAccountService accountService = new FakeAccountService();
        AuthController controller = new AuthController(authenticationService, accountService);
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("old-secret");
        request.setNewPassword("new-secret");

        ResponseEntity<ApiResponse<AccountResponse>> response = controller.updatePassword(null, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(accountService.updatedAccountId).isNull();
    }

    private Account account() {
        Account account = new Account();
        account.setId("user-1");
        account.setEmail("user@example.com");
        account.setFullName("Test User");
        account.setIsActive(true);
        account.setIsEmailVerified(true);
        account.setMustChangePassword(false);
        return account;
    }

    private static class FakeAuthenticationService extends AuthenticationService {
        private Optional<Account> authenticatedAccount = Optional.empty();
        private Optional<Account> accountByToken = Optional.empty();
        private boolean tokenValid;
        private String loggedOutToken;

        @Override
        public Optional<Account> authenticateByEmailStrict(String email, String password) {
            return authenticatedAccount;
        }

        @Override
        public String generateToken(Account account) {
            return "jwt-token";
        }

        @Override
        public long getTokenExpirationEpochSeconds(String token) {
            return 123456789L;
        }

        @Override
        public Optional<Account> getAuthenticatedAccountByToken(String token) {
            return accountByToken;
        }

        @Override
        public boolean validateToken(String token) {
            return tokenValid;
        }

        @Override
        public void logout(String token) {
            loggedOutToken = token;
        }
    }

    private static class FakeAccountService extends AccountService {
        private String updatedAccountId;
        private String oldPassword;
        private String newPassword;

        @Override
        public Account updatePassword(String accountId, String oldPassword, String newPassword) {
            this.updatedAccountId = accountId;
            this.oldPassword = oldPassword;
            this.newPassword = newPassword;
            return account();
        }

        private Account account() {
            Account account = new Account();
            account.setId("user-1");
            account.setEmail("user@example.com");
            account.setFullName("Test User");
            account.setIsActive(true);
            account.setIsEmailVerified(true);
            account.setMustChangePassword(false);
            return account;
        }
    }
}
