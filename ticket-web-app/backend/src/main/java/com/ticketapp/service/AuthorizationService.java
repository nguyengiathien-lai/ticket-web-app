package com.ticketapp.service;

import com.ticketapp.entity.Account;
import com.ticketapp.entity.Permission;
import com.ticketapp.entity.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AuthorizationService {

    @Autowired
    private AccountService accountService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PermissionService permissionService;

    /**
     * Check if account has specific role
     */
    public boolean hasRole(String accountId, String roleName) {
        return accountService.hasRole(accountId, roleName);
    }

    /**
     * Check if account has any of the specified roles
     */
    public boolean hasAnyRole(String accountId, String... roleNames) {
        Account account = accountService.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        for (String roleName : roleNames) {
            if (account.getRoles().stream()
                    .anyMatch(role -> role.getName().equals(roleName))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if account has all specified roles
     */
    public boolean hasAllRoles(String accountId, String... roleNames) {
        Account account = accountService.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        for (String roleName : roleNames) {
            if (account.getRoles().stream()
                    .noneMatch(role -> role.getName().equals(roleName))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if account has specific permission (through any role)
     */
    public boolean hasPermission(String accountId, String permissionName) {
        Account account = accountService.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        return account.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .anyMatch(permission -> permission.getName().equals(permissionName));
    }

    /**
     * Check if account has any of the specified permissions
     */
    public boolean hasAnyPermission(String accountId, String... permissionNames) {
        Account account = accountService.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        Set<String> accountPermissions = account.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());

        for (String permission : permissionNames) {
            if (accountPermissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if account has all specified permissions
     */
    public boolean hasAllPermissions(String accountId, String... permissionNames) {
        Account account = accountService.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        Set<String> accountPermissions = account.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());

        for (String permission : permissionNames) {
            if (!accountPermissions.contains(permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get all permissions for account (through all roles)
     */
    public Set<String> getAccountPermissions(String accountId) {
        Account account = accountService.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        return account.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Get all roles for account
     */
    public Set<String> getAccountRoles(String accountId) {
        Account account = accountService.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        return account.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }

    /**
     * Check if account is admin
     */
    public boolean isAdmin(String accountId) {
        return hasRole(accountId, "APP_ADMIN");
    }

    /**
     * Check if account is passenger
     */
    public boolean isPassenger(String accountId) {
        return hasRole(accountId, "PASSENGER");
    }

    /**
     * Verify authorization - throws exception if not authorized
     */
    public void requirePermission(String accountId, String permissionName) {
        if (!hasPermission(accountId, permissionName)) {
            log.warn("Authorization denied: Account {} does not have permission {}", 
                    accountId, permissionName);
            throw new SecurityException("Permission denied: " + permissionName);
        }
    }

    /**
     * Verify authorization for any permission - throws exception if not authorized
     */
    public void requireAnyPermission(String accountId, String... permissionNames) {
        if (!hasAnyPermission(accountId, permissionNames)) {
            log.warn("Authorization denied: Account {} does not have any of the permissions", accountId);
            throw new SecurityException("Permission denied");
        }
    }

    /**
     * Verify authorization for all permissions - throws exception if not authorized
     */
    public void requireAllPermissions(String accountId, String... permissionNames) {
        if (!hasAllPermissions(accountId, permissionNames)) {
            log.warn("Authorization denied: Account {} does not have all required permissions", accountId);
            throw new SecurityException("Permission denied");
        }
    }

    /**
     * Verify role - throws exception if not authorized
     */
    public void requireRole(String accountId, String roleName) {
        if (!hasRole(accountId, roleName)) {
            log.warn("Authorization denied: Account {} does not have role {}", accountId, roleName);
            throw new SecurityException("Role required: " + roleName);
        }
    }

    /**
     * Verify admin role - throws exception if not authorized
     */
    public void requireAdmin(String accountId) {
        requireRole(accountId, "APP_ADMIN");
    }
}
