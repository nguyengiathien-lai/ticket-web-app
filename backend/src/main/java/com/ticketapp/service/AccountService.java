package com.ticketapp.service;

import com.ticketapp.entity.Account;
import com.ticketapp.entity.Role;
import com.ticketapp.repository.AccountRepository;
import com.ticketapp.repository.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Register a new account
     */
    @Transactional
    public Account registerAccount(String email, String password, String fullName, 
                                   String phoneNumber, String personalId, String address) {
        // Validate email is unique
        if (accountRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        // Validate phone is unique
        if (phoneNumber != null && accountRepository.existsByPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("Phone number already registered");
        }

        // Validate personal ID is unique
        if (personalId != null && accountRepository.existsByPersonalId(personalId)) {
            throw new IllegalArgumentException("Personal ID already registered");
        }

        Account account = new Account();
        account.setId(UUID.randomUUID().toString());
        account.setEmail(email);
        account.setPassword(passwordEncoder.encode(password));
        account.setFullName(fullName);
        account.setPhoneNumber(phoneNumber);
        account.setPersonalId(personalId);
        account.setAddress(address);
        account.setIsActive(true);
        account.setIsEmailVerified(false);
        account.setMustChangePassword(false);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());

        // Assign default PASSENGER role
        Role passengerRole = roleRepository.findByName("PASSENGER")
                .orElseThrow(() -> new RuntimeException("PASSENGER role not found"));
        account.getRoles().add(passengerRole);

        Account savedAccount = accountRepository.save(account);
        log.info("Account registered: {} ({})", savedAccount.getId(), savedAccount.getEmail());

        return savedAccount;
    }

    /**
     * Verify account email
     */
    @Transactional
    public Account verifyEmail(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        account.setIsEmailVerified(true);
        account.setUpdatedAt(LocalDateTime.now());

        Account updated = accountRepository.save(account);
        log.info("Email verified for account: {}", accountId);

        return updated;
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
            throw new IllegalArgumentException("Old password is incorrect");
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
