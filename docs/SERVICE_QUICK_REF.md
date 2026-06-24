# Authentication & Authorization Services - Quick Reference

## Services Created ✅

| Service | Purpose | Key Methods |
|---------|---------|-------------|
| **AccountService** | User registration, password management, role assignment | `registerAccount()`, `verifyEmail()`, `updatePassword()`, `assignRole()`, `hasRole()` |
| **RoleService** | Role management, permission assignment | `createRole()`, `addPermissionToRole()`, `getRolePermissions()` |
| **PermissionService** | Permission management | `createPermission()`, `searchByKeyword()`, `getAllPermissions()` |
| **AuthenticationService** | Login, token validation, account access | `authenticateByEmail()`, `isAccountValid()`, `authorize()`, `logout()` |
| **AuthorizationService** | Permission & role checks, authorization | `hasPermission()`, `hasRole()`, `requirePermission()`, `getAccountPermissions()` |

---

## Quick Usage Examples

### 1. Register User
```java
@Autowired
private AccountService accountService;

// Register new account (auto-assigns PASSENGER role)
Account account = accountService.registerAccount(
    "user@example.com",
    "password123",
    "John Doe",
    "0987654321",
    "ID123456",
    "123 Main St"
);
```

### 2. Login
```java
@Autowired
private AuthenticationService authService;

// Authenticate user
Optional<Account> user = authService.authenticateByEmail(
    "user@example.com",
    "password123"
);

if (user.isPresent()) {
    // Generate JWT token here
    String token = generateJWT(user.get());
}
```

### 3. Check Permission
```java
@Autowired
private AuthorizationService authService;

// Check if user has permission (boolean)
if (authService.hasPermission(accountId, "TICKET_VIEW")) {
    // Display tickets
}

// Strict check (throws exception if denied)
try {
    authService.requirePermission(accountId, "TICKET_CREATE");
    // Create ticket
} catch (SecurityException e) {
    return ResponseEntity.status(403).build();
}
```

### 4. Check Role
```java
// Check if admin
if (authService.isAdmin(accountId)) {
    // Show admin panel
}

// Check any role
if (authService.hasAnyRole(accountId, "APP_ADMIN", "SUPPORT")) {
    // Show management page
}
```

### 5. Manage Roles
```java
@Autowired
private AccountService accountService;

// Assign role to user
accountService.assignRole(accountId, "APP_ADMIN");

// Remove role
accountService.removeRole(accountId, "APP_ADMIN");

// Get all roles
Set<Role> roles = accountService.getAccountRoles(accountId);
```

### 6. Password Management
```java
// Change password (requires old password)
accountService.updatePassword(accountId, "oldPass", "newPass");

// Reset password (admin only)
accountService.resetPassword(accountId, "newPass");
// User must change password on next login
```

---

## Method Reference by Service

### AccountService
```java
// Registration & Setup
registerAccount(email, password, fullName, phone, personalId, address)
verifyEmail(accountId)
updatePassword(accountId, oldPassword, newPassword)
resetPassword(accountId, newPassword)

// Lookup
findByEmail(email)
findById(accountId)
findByPhoneNumber(phoneNumber)
findActiveByEmail(email)
getAllVerifiedAndActive()

// Role Management
assignRole(accountId, roleName)
removeRole(accountId, roleName)
getAccountRoles(accountId)
hasRole(accountId, roleName)

// Status
updateStatus(accountId, isActive)
deactivateAccount(accountId)
activateAccount(accountId)

// Utilities
verifyPassword(rawPassword, encodedPassword)
emailExists(email)
phoneNumberExists(phoneNumber)
getActiveAccountCount()
getUnverifiedAccountCount()
getAccountsRequiringPasswordChange()
```

### AuthenticationService
```java
authenticateByEmail(email, password)
authenticateByEmailStrict(email, password)
isAccountValid(accountId)
authorize(accountId, permissionName)
authorizeRole(accountId, roleName)
getAuthenticatedAccount(accountId)
isPasswordChangeRequired(accountId)
validateToken(accountId)
logout(accountId)
```

### AuthorizationService
```java
// Role Checks
hasRole(accountId, roleName)
hasAnyRole(accountId, roleNames...)
hasAllRoles(accountId, roleNames...)
isAdmin(accountId)
isPassenger(accountId)

// Permission Checks
hasPermission(accountId, permissionName)
hasAnyPermission(accountId, permissionNames...)
hasAllPermissions(accountId, permissionNames...)

// Information
getAccountRoles(accountId)
getAccountPermissions(accountId)

// Strict Authorization (throws exception)
requireRole(accountId, roleName)
requireAdmin(accountId)
requirePermission(accountId, permissionName)
requireAnyPermission(accountId, permissionNames...)
requireAllPermissions(accountId, permissionNames...)
```

### RoleService
```java
createRole(name, description)
findByName(name)
findById(roleId)
findByIdWithPermissions(roleId)
getAllRoles()
getAllRolesWithPermissions()
findRolesByNames(roleNames)
addPermissionToRole(roleId, permissionId)
addPermissionToRoleByName(roleName, permissionName)
removePermissionFromRole(roleId, permissionId)
removePermissionFromRoleByName(roleName, permissionName)
getRolePermissions(roleId)
getRolePermissionsByName(roleName)
roleExists(roleName)
updateRoleDescription(roleId, description)
```

### PermissionService
```java
createPermission(name, description)
findByName(name)
findById(permissionId)
getAllPermissions()
findPermissionsByNames(permissionNames)
searchByKeyword(keyword)
permissionExists(name)
updatePermissionDescription(permissionId, description)
getPermissionCount()
```

---

## Common Authorization Patterns

### Pattern 1: Controller with Permission Check
```java
@PostMapping("/api/tickets")
public ResponseEntity<?> createTicket(
        @RequestParam String accountId,
        @RequestBody TicketRequest request) {
    
    // Check permission
    if (!authService.hasPermission(accountId, "TICKET_CREATE")) {
        return ResponseEntity.status(403).body("Permission denied");
    }
    
    // Create ticket
    return ResponseEntity.ok(ticketService.create(request));
}
```

### Pattern 2: Strict Authorization
```java
@PostMapping("/api/tickets")
public ResponseEntity<?> createTicket(@RequestBody TicketRequest request) {
    String accountId = getCurrentAccountId(); // From JWT
    
    try {
        authService.requirePermission(accountId, "TICKET_CREATE");
        return ResponseEntity.ok(ticketService.create(request));
    } catch (SecurityException e) {
        return ResponseEntity.status(403).build();
    }
}
```

### Pattern 3: Multiple Permissions
```java
// User must have ANY of these permissions
if (authService.hasAnyPermission(accountId, 
    "TICKET_CREATE", "TICKET_EDIT", "TICKET_DELETE")) {
    // Show ticket management
}

// User must have ALL of these permissions
if (authService.hasAllPermissions(accountId,
    "TICKET_CREATE", "PAYMENT_PROCESS")) {
    // Strict operation requiring multiple permissions
}
```

### Pattern 4: Role-Based Access
```java
@GetMapping("/api/admin/dashboard")
public ResponseEntity<?> getAdminDashboard() {
    String accountId = getCurrentAccountId();
    
    try {
        authService.requireAdmin(accountId);
        return ResponseEntity.ok(adminService.getDashboard());
    } catch (SecurityException e) {
        return ResponseEntity.status(403).build();
    }
}
```

---

## Transaction & Thread Safety

All service methods using `@Transactional` are:
- ✅ Thread-safe
- ✅ Atomic (all-or-nothing)
- ✅ Automatically rolled back on exception
- ✅ Managed by Spring

---

## Logging

All services use SLF4J (`@Slf4j`):

```
[INFO] Authentication successful for: user@example.com
[WARN] Authentication failed: Invalid password for email: user@example.com
[WARN] Authorization denied: Account abc does not have permission TICKET_CREATE
[INFO] Account registered: d1a2b3c4 (user@example.com)
[INFO] Role ADMIN assigned to account: d1a2b3c4
```

---

## Error Handling

```java
// Account not found
Optional<Account> account = accountService.findById("invalid-id");
if (account.isEmpty()) {
    // Handle not found
}

// Invalid input (throws IllegalArgumentException)
try {
    accountService.registerAccount(...);
} catch (IllegalArgumentException e) {
    // Email exists, phone exists, or invalid input
}

// Authorization denied (throws SecurityException)
try {
    authService.requirePermission(accountId, "ADMIN_PANEL");
} catch (SecurityException e) {
    // Permission denied
}
```

---

## Integration Files

📝 Created Documentation:
- `SERVICE_GUIDE.md` - Comprehensive guide with examples
- `REPOSITORY_GUIDE.md` - Repository documentation
- `REPOSITORY_QUICK_REF.md` - Quick reference

📂 Service Files:
```
backend/src/main/java/com/ticketapp/service/
├── AccountService.java           (User management)
├── AuthenticationService.java    (Login & validation)
├── AuthorizationService.java     (Permission checks)
├── RoleService.java              (Role management)
└── PermissionService.java        (Permission management)
```

---

## Next Steps

1. ✅ Services created
2. ⏳ Create DTOs (LoginRequest, RegisterRequest, etc.)
3. ⏳ Create Controllers
4. ⏳ Add Spring Security config
5. ⏳ Implement JWT token generation
6. ⏳ Add @PreAuthorize annotations

Ready to create controllers? 🚀
