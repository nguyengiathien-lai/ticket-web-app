package com.ticketapp.service;

import com.ticketapp.dto.account.ProfileUpdateRequest;
import com.ticketapp.entity.Account;
import com.ticketapp.entity.Role;
import com.ticketapp.repository.AccountRepository;
import com.ticketapp.repository.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OtpService otpService;

    @Autowired
    private EmailService emailService;

    /**
     * Register a new account
     */
    @Transactional
    public Account registerAccount(String email, String password, String firstName, String lastName) {
        // Validate email is unique
        if (accountRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã được đăng ký. Vui lòng sử dụng email khác.");
        }

        Account account = new Account();
        account.setId(UUID.randomUUID().toString());
        account.setEmail(email);
        account.setPassword(passwordEncoder.encode(password));
        account.setFullName(buildFullName(firstName, lastName));
        account.setIsActive(true);
        account.setIsEmailVerified(false);
        account.setMustChangePassword(false);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());

        // Assign default PASSENGER role
        Role passengerRole = roleRepository.findByName("PASSENGER")
                .orElseThrow(() -> new RuntimeException("PASSENGER role not found"));
        account.getRoles().add(passengerRole);

        Account savedAccount = accountRepository.saveAndFlush(account);
        otpService.sendEmailVerificationOtp(savedAccount);
        log.info("Account registered: {} ({})", savedAccount.getId(), savedAccount.getEmail());

        return savedAccount;
    }

    private String buildFullName(String firstName, String lastName) {
        return (firstName.trim() + " " + lastName.trim()).trim();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    /**
     * Verify account email using an OTP sent during registration
     */
    @Transactional
    public Account verifyEmailWithOtp(String email, String code) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (account.getIsEmailVerified()) {
            return account;
        }

        otpService.verifyEmailOtp(account, code);

        account.setIsEmailVerified(true);
        account.setUpdatedAt(LocalDateTime.now());

        Account updated = accountRepository.save(account);
        log.info("Email verified with OTP for account: {}", account.getId());
        runAfterCommit(() -> sendAccountRegistrationConfirmation(updated));

        return updated;
    }

    private void sendAccountRegistrationConfirmation(Account account) {
        try {
            emailService.sendAccountRegistrationConfirmed(account.getEmail(), account.getFullName());
        } catch (RuntimeException exception) {
            log.warn("Account registration confirmation email failed for account: {}", account.getId(), exception);
        }
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    /**
     * Send a new email verification OTP for an unverified account
     */
    @Transactional
    public void resendEmailVerificationOtp(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (!account.getIsActive()) {
            throw new IllegalArgumentException("Account is inactive");
        }

        if (account.getIsEmailVerified()) {
            throw new IllegalArgumentException("Email is already verified");
        }
      
        otpService.sendEmailVerificationOtp(account);
        log.info("Email verification OTP resent for account: {}", account.getId());
    }

    /**
     * Update password
     */
    @Transactional
    public Account updatePassword(String accountId, String oldPassword, String newPassword) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, account.getPassword())) {
            throw new IllegalArgumentException("Mât khẩu cũ không đúng. Vui lòng thử lại.");
        }

        // Update password
        account.setPassword(passwordEncoder.encode(newPassword));
        account.setMustChangePassword(false);
        account.setUpdatedAt(LocalDateTime.now());

        Account updated = accountRepository.save(account);
        log.info("Password updated for account: {}", accountId);

        return updated;
    }

    /**
     * Update optional passenger profile fields.
     */
    @Transactional
    public Account updateProfile(String accountId, ProfileUpdateRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        String phoneNumber = blankToNull(request.getPhoneNumber());
        if (phoneNumber != null) {
            accountRepository.findByPhoneNumber(phoneNumber)
                    .filter(existing -> !existing.getId().equals(accountId))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Phone number already registered");
                    });
        }

        String personalId = blankToNull(request.getPersonalId());
        if (personalId != null) {
            accountRepository.findByPersonalId(personalId)
                    .filter(existing -> !existing.getId().equals(accountId))
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException("Personal ID already registered");
                    });
        }

        String fullName = blankToNull(request.getFullName());
        if (fullName != null) {
            account.setFullName(fullName);
        }
        account.setPhoneNumber(phoneNumber);
        account.setPersonalId(personalId);
        account.setAddress(blankToNull(request.getAddress()));
        account.setDateOfBirth(request.getDateOfBirth());
        account.setGender(blankToNull(request.getGender()));
        account.setUpdatedAt(LocalDateTime.now());

        Account updated = accountRepository.save(account);
        log.info("Profile updated for account: {}", accountId);

        return updated;
    }

    /**
     * Reset password (admin or forgot password flow)
     */
    @Transactional
    public Account resetPassword(String accountId, String newPassword) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        account.setPassword(passwordEncoder.encode(newPassword));
        account.setMustChangePassword(true);
        account.setUpdatedAt(LocalDateTime.now());

        Account updated = accountRepository.save(account);
        log.info("Password reset for account: {}", accountId);

        return updated;
    }

    /**
     * Find account by email
     */
    public Optional<Account> findByEmail(String email) {
        return accountRepository.findByEmail(email);
    }

    /**
     * Find active account by email
     */
    public Optional<Account> findActiveByEmail(String email) {
        return accountRepository.findActiveByEmail(email);
    }

    /**
     * Find account by ID
     */
    public Optional<Account> findById(String accountId) {
        return accountRepository.findById(accountId);
    }

    /**
     * Find by phone number
     */
    public Optional<Account> findByPhoneNumber(String phoneNumber) {
        return accountRepository.findByPhoneNumber(phoneNumber);
    }

    /**
     * Get all verified and active accounts
     */
    public List<Account> getAllVerifiedAndActive() {
        return accountRepository.findAllVerifiedAndActive();
    }

    /**
     * Get all accounts for system admin management
     */
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    /**
     * Assign role to account
     */
    @Transactional
    public Account assignRole(String accountId, String roleName) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));

        if (account.getRoles().contains(role)) {
            log.warn("Role {} already assigned to account {}", roleName, accountId);
            return account;
        }

        account.getRoles().add(role);
        account.setUpdatedAt(LocalDateTime.now());

        Account updated = accountRepository.save(account);
        log.info("Role {} assigned to account: {}", roleName, accountId);

        return updated;
    }

    /**
     * Remove role from account
     */
    @Transactional
    public Account removeRole(String accountId, String roleName) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));

        account.getRoles().remove(role);
        account.setUpdatedAt(LocalDateTime.now());

        Account updated = accountRepository.save(account);
        log.info("Role {} removed from account: {}", roleName, accountId);

        return updated;
    }

    /**
     * Update account status (active/inactive)
     */
    @Transactional
    public Account updateStatus(String accountId, Boolean isActive) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        account.setIsActive(isActive);
        account.setUpdatedAt(LocalDateTime.now());

        Account updated = accountRepository.save(account);
        log.info("Account {} status updated to: {}", accountId, isActive ? "ACTIVE" : "INACTIVE");

        return updated;
    }

    /**
     * Deactivate account
     */
    @Transactional
    public Account deactivateAccount(String accountId) {
        return updateStatus(accountId, false);
    }

    /**
     * Activate account
     */
    @Transactional
    public Account activateAccount(String accountId) {
        return updateStatus(accountId, true);
    }

    /**
     * Update email verification status
     */
    @Transactional
    public Account updateEmailVerificationStatus(String accountId, Boolean isEmailVerified) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        account.setIsEmailVerified(isEmailVerified);
        account.setUpdatedAt(LocalDateTime.now());

        Account updated = accountRepository.save(account);
        log.info("Account {} email verification status updated to: {}", accountId, isEmailVerified);

        return updated;
    }

    /**
     * Permanently delete an account
     */
    @Transactional
    public void deleteAccount(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        otpService.deleteEmailVerificationOtp(account.getId());
        account.getRoles().clear();
        accountRepository.delete(account);
        log.info("Account deleted: {}", accountId);
    }

    /**
     * Get account count statistics
     */
    public long getActiveAccountCount() {
        return accountRepository.countActiveAccounts();
    }

    /**
     * Get unverified account count
     */
    public long getUnverifiedAccountCount() {
        return accountRepository.countUnverifiedAccounts();
    }

    /**
     * Get all accounts requiring password change
     */
    public List<Account> getAccountsRequiringPasswordChange() {
        return accountRepository.findAccountsRequiringPasswordChange();
    }

    /**
     * Check if account has role
     */
    public boolean hasRole(String accountId, String roleName) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        return account.getRoles().stream()
                .anyMatch(role -> role.getName().equals(roleName));
    }

    /**
     * Get account roles
     */
    public Set<Role> getAccountRoles(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        return account.getRoles();
    }

    /**
     * Verify password
     */
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * Check if email exists
     */
    public boolean emailExists(String email) {
        return accountRepository.existsByEmail(email);
    }

    /**
     * Check if phone number exists
     */
    public boolean phoneNumberExists(String phoneNumber) {
        return accountRepository.existsByPhoneNumber(phoneNumber);
    }
}
