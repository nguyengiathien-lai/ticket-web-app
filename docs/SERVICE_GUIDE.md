# Authentication & Authorization Services

## Overview

Five core service classes have been created to handle authentication, authorization, roles, permissions, and accounts.

---

## 1. AccountService

**Location**: `com.ticketapp.service.AccountService`

### Account Management Methods

#### Registration & Setup
- `registerAccount(email, password, fullName, phoneNumber, personalId, address): Account`
  - Validates unique email, phone, personalId
  - Hashes password using PasswordEncoder
  - Automatically assigns PASSENGER role
  - Returns created Account

- `verifyEmail(accountId): Account` - Mark email as verified

- `updatePassword(accountId, oldPassword, newPassword): Account`
  - Verifies old password
  - Updates to new hashed password
  - Clears mustChangePassword flag

- `resetPassword(accountId, newPassword): Account`
  - Sets new password (for admin/forgot flow)
  - Sets mustChangePassword = true

#### Account Lookup
- `findByEmail(email): Optional<Account>`
- `findById(accountId): Optional<Account>`
- `findByPhoneNumber(phoneNumber): Optional<Account>`
- `findActiveByEmail(email): Optional<Account>` - Only active accounts
- `getAllVerifiedAndActive(): List<Account>` - Get all verified accounts

#### Role Management
- `assignRole(accountId, roleName): Account` - Add role to account
- `removeRole(accountId, roleName): Account` - Remove role from account
- `getAccountRoles(accountId): Set<Role>` - Get all roles
- `hasRole(accountId, roleName): boolean` - Check if has role

#### Status Management
- `updateStatus(accountId, isActive): Account` - Set active/inactive
- `deactivateAccount(accountId): Account` - Deactivate
- `activateAccount(accountId): Account` - Activate

#### Utilities
- `verifyPassword(rawPassword, encodedPassword): boolean`
- `emailExists(email): boolean`
- `phoneNumberExists(phoneNumber): boolean`
- `getActiveAccountCount(): long`
- `getUnverifiedAccountCount(): long`
- `getAccountsRequiringPasswordChange(): List<Account>`

### Usage Example
```java
@Autowired
private AccountService accountService;

// Register new account
Account account = accountService.registerAccount(
    "user@example.com",
    "password123",
    "John Doe",
    "0987654321",
    "123456789",
    "123 Main St"
);

// Authenticate
Optional<Account> user = authService.authenticateByEmail("user@example.com", "password123");

// Manage roles
accountService.assignRole(account.getId(), "PASSENGER");
boolean isPassenger = accountService.hasRole(account.getId(), "PASSENGER");

// Change password
accountService.updatePassword(account.getId(), "oldPass", "newPass");
```

---

## 2. RoleService

**Location**: `com.ticketapp.service.RoleService`

### Role Management Methods

#### Create & Find
- `createRole(name, description): Role` - Create new role
- `findByName(name): Optional<Role>`
- `findById(roleId): Optional<Role>`
- `findByIdWithPermissions(roleId): Optional<Role>` - Eager load permissions
- `getAllRoles(): List<Role>`
- `getAllRolesWithPermissions(): List<Role>` - All roles with permissions
- `findRolesByNames(roleNames): Set<Role>`

#### Permission Management
- `addPermissionToRole(roleId, permissionId): Role`
- `addPermissionToRoleByName(roleName, permissionName): Role`
- `removePermissionFromRole(roleId, permissionId): Role`
- `removePermissionFromRoleByName(roleName, permissionName): Role`
- `getRolePermissions(roleId): Set<Permission>`
- `getRolePermissionsByName(roleName): Set<Permission>`

#### Utilities
- `roleExists(roleName): boolean`
- `updateRoleDescription(roleId, description): Role`

### Usage Example
```java
@Autowired
private RoleService roleService;

// Create role
Role adminRole = roleService.createRole("ADMIN", "Administrator role");

// Add permissions to role
roleService.addPermissionToRoleByName("ADMIN", "ACCOUNT_MANAGE");
roleService.addPermissionToRoleByName("ADMIN", "ROLE_MANAGE");

// Get role with permissions
Optional<Role> role = roleService.findByIdWithPermissions(1);
```

---

## 3. PermissionService

**Location**: `com.ticketapp.service.PermissionService`

### Permission Management Methods

#### Create & Find
- `createPermission(name, description): Permission` - Create new permission
- `findByName(name): Optional<Permission>`
- `findById(permissionId): Optional<Permission>`
- `getAllPermissions(): List<Permission>`
- `findPermissionsByNames(permissionNames): Set<Permission>`

#### Search & Utilities
- `searchByKeyword(keyword): List<Permission>` - Search permissions
- `permissionExists(name): boolean`
- `updatePermissionDescription(permissionId, description): Permission`
- `getPermissionCount(): long`

### Usage Example
```java
@Autowired
private PermissionService permissionService;

// Create permission
Permission perm = permissionService.createPermission(
    "TICKET_VIEW",
    "View tickets"
);

// Search permissions
List<Permission> ticketPerms = permissionService.searchByKeyword("TICKET");

// Find multiple permissions
Set<Permission> perms = permissionService.findPermissionsByNames(
    Arrays.asList("TICKET_VIEW", "TICKET_CREATE")
);
```

---

## 4. AuthorizationService

**Location**: `com.ticketapp.service.AuthorizationService`

### Authorization Check Methods

#### Role Checks
- `hasRole(accountId, roleName): boolean`
- `hasAnyRole(accountId, roleNames...): boolean` - Check if has ANY role
- `hasAllRoles(accountId, roleNames...): boolean` - Check if has ALL roles
- `isAdmin(accountId): boolean` - Check if admin
- `isPassenger(accountId): boolean` - Check if passenger

#### Permission Checks
- `hasPermission(accountId, permissionName): boolean`
- `hasAnyPermission(accountId, permissionNames...): boolean`
- `hasAllPermissions(accountId, permissionNames...): boolean`

#### Information Retrieval
- `getAccountRoles(accountId): Set<String>` - Get all role names
- `getAccountPermissions(accountId): Set<String>` - Get all permission names

#### Strict Authorization (throws exception)
- `requireRole(accountId, roleName): void` - Throws SecurityException if no role
- `requireAdmin(accountId): void` - Throws if not admin
- `requirePermission(accountId, permissionName): void`
- `requireAnyPermission(accountId, permissionNames...): void`
- `requireAllPermissions(accountId, permissionNames...): void`

### Usage Example
```java
@Autowired
private AuthorizationService authService;

// Check permissions
if (authService.hasPermission(accountId, "TICKET_VIEW")) {
    // Show tickets
}

// Multiple checks
boolean canManage = authService.hasAnyPermission(
    accountId, 
    "TICKET_CREATE", 
    "TICKET_EDIT"
);

// Strict authorization (in controller)
try {
    authService.requirePermission(accountId, "TICKET_CREATE");
    // Create ticket
} catch (SecurityException e) {
    return ResponseEntity.status(403).build();
}
```

---

## 5. AuthenticationService

**Location**: `com.ticketapp.service.AuthenticationService`

### Authentication Methods

#### Login & Validation
- `authenticateByEmail(email, password): Optional<Account>`
  - Checks account active + email verified
  - Verifies password
  - Returns Account if successful

- `authenticateByEmailStrict(email, password): Optional<Account>`
  - Same as above
  - Also checks mustChangePassword flag

- `isAccountValid(accountId): boolean` - Check if account is active & verified

#### Authorization & Access Control
- `authorize(accountId, permissionName): boolean` - Check permission (non-exception)
- `authorizeRole(accountId, roleName): boolean` - Check role (non-exception)
- `getAuthenticatedAccount(accountId): Optional<Account>` - Get only if valid

#### Session & Token Management
- `isPasswordChangeRequired(accountId): boolean`
- `validateToken(accountId): boolean` - Token validation (future JWT)
- `logout(accountId): void` - Logout (token blacklist in production)

### Usage Example
```java
@Autowired
private AuthenticationService authService;

// Login
Optional<Account> account = authService.authenticateByEmail(
    "user@example.com",
    "password123"
);

if (account.isPresent()) {
    Account user = account.get();
    if (authService.isPasswordChangeRequired(user.getId())) {
        // Redirect to password change page
    }
    // Generate JWT token and login
}

// Authorization check
if (authService.authorize(accountId, "TICKET_VIEW")) {
    // Show tickets
}
```

---

## Service Hierarchy

```
┌─────────────────────────────────────┐
│   AuthenticationService             │
│   (Login, Token Validation)         │
│   ├─ authenticateByEmail()          │
│   ├─ authorize()                    │
│   └─ validateToken()                │
└──────────────┬──────────────────────┘
               │ uses
               ▼
┌──────────────────────────────────────┐
│   AuthorizationService              │
│   (Permission & Role Checks)        │
│   ├─ hasPermission()                │
│   ├─ hasRole()                      │
│   └─ requirePermission()            │
└──────────────┬──────────────────────┘
       ▲       │ uses
       │       ▼
       │   ┌─────────────────┐
       │   │ AccountService  │
       │   │ (User Mgmt)     │
       │   └────────┬────────┘
       │            │
       └────────────┘
            uses
               │
       ┌───────┴──────────┬──────────────┐
       ▼                  ▼              ▼
    ┌─────────────┐  ┌──────────────┐  ┌──────────────────┐
    │ AccountRepo │  │ RoleService  │  │ PermissionService│
    └─────────────┘  └──────────────┘  └──────────────────┘
```

---

## Features

✅ **Security**
- Password hashing (PasswordEncoder)
- Permission-based authorization
- Role-based access control
- Account status management
- Email verification

✅ **Account Management**
- User registration
- Password update/reset
- Account activation/deactivation
- Role assignment

✅ **Authorization**
- Multiple permission checks
- Role verification
- Strict authorization (exceptions)
- Non-strict authorization (boolean)

✅ **Transaction Management**
- @Transactional for data consistency
- Automatic rollback on errors

✅ **Logging**
- All operations logged with @Slf4j
- Security events tracked

---

## Integration with Controllers

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthenticationService authService;
    
    @Autowired
    private AccountService accountService;
    
    @Autowired
    private AuthorizationService authorizationService;
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<Account> account = authService.authenticateByEmail(
            request.getEmail(),
            request.getPassword()
        );
        
        if (account.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
        
        // Generate JWT token and return
        return ResponseEntity.ok(new LoginResponse(account.get()));
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        Account account = accountService.registerAccount(
            request.getEmail(),
            request.getPassword(),
            request.getFullName(),
            request.getPhoneNumber(),
            request.getPersonalId(),
            request.getAddress()
        );
        
        return ResponseEntity.status(201).body(account);
    }
    
    @GetMapping("/verify-permission")
    public ResponseEntity<?> verifyPermission(
            @RequestParam String accountId,
            @RequestParam String permission) {
        
        try {
            authorizationService.requirePermission(accountId, permission);
            return ResponseEntity.ok("Authorized");
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }
}
```

---

## Error Handling

### Common Exceptions
- `IllegalArgumentException` - Invalid input (email exists, role not found)
- `SecurityException` - Authorization failed (thrown by require* methods)
- `RuntimeException` - System errors (PASSENGER role not found)

### Usage
```java
try {
    accountService.registerAccount(email, password, fullName, 
                                   phone, personalId, address);
} catch (IllegalArgumentException e) {
    // Handle validation error
    return ResponseEntity.status(400).body(e.getMessage());
}

try {
    authService.requirePermission(accountId, "TICKET_CREATE");
} catch (SecurityException e) {
    // Handle authorization error
    return ResponseEntity.status(403).body("Permission denied");
}
```

---

## Next Steps

1. ✅ Services created
2. ⏳ Create DTOs for API payloads
3. ⏳ Create REST Controllers
4. ⏳ Add Spring Security configuration
5. ⏳ Implement JWT authentication
6. ⏳ Add @PreAuthorize annotations

