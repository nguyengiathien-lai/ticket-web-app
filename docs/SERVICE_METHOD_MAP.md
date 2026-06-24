# Service Method Quick Map

## AccountService - User Management

```
┌─ REGISTRATION
│  └─ registerAccount(email, password, fullName, phone, personalId, address)
│     Returns: Account with PASSENGER role auto-assigned
│
├─ VERIFICATION
│  └─ verifyEmail(accountId)
│     Returns: Account with isEmailVerified=true
│
├─ PASSWORD
│  ├─ updatePassword(accountId, oldPassword, newPassword)
│  │  - Requires old password verification
│  │  - Clears mustChangePassword flag
│  │
│  └─ resetPassword(accountId, newPassword)
│     - Sets mustChangePassword=true
│     - Admin flow only
│
├─ LOOKUP
│  ├─ findByEmail(email) → Optional<Account>
│  ├─ findById(accountId) → Optional<Account>
│  ├─ findByPhoneNumber(phone) → Optional<Account>
│  ├─ findActiveByEmail(email) → Optional<Account>
│  └─ getAllVerifiedAndActive() → List<Account>
│
├─ ROLE MANAGEMENT
│  ├─ assignRole(accountId, roleName) → Account
│  ├─ removeRole(accountId, roleName) → Account
│  ├─ getAccountRoles(accountId) → Set<Role>
│  └─ hasRole(accountId, roleName) → boolean
│
├─ STATUS
│  ├─ updateStatus(accountId, isActive) → Account
│  ├─ activateAccount(accountId) → Account
│  └─ deactivateAccount(accountId) → Account
│
└─ UTILITIES
   ├─ verifyPassword(raw, encoded) → boolean
   ├─ emailExists(email) → boolean
   ├─ phoneNumberExists(phone) → boolean
   ├─ getActiveAccountCount() → long
   ├─ getUnverifiedAccountCount() → long
   └─ getAccountsRequiringPasswordChange() → List<Account>
```

## AuthenticationService - Login & Validation

```
┌─ LOGIN
│  ├─ authenticateByEmail(email, password) → Optional<Account>
│  │  Checks: active, email verified, password match
│  │
│  └─ authenticateByEmailStrict(email, password) → Optional<Account>
│     Checks: above + mustChangePassword flag
│
├─ VALIDATION
│  ├─ isAccountValid(accountId) → boolean
│  │  Checks: active & email verified
│  │
│  ├─ isPasswordChangeRequired(accountId) → boolean
│  │
│  └─ validateToken(accountId) → boolean
│     Account exists & active
│
├─ AUTHORIZATION
│  ├─ authorize(accountId, permissionName) → boolean
│  │  Non-throwing version
│  │
│  └─ authorizeRole(accountId, roleName) → boolean
│     Non-throwing version
│
├─ ACCESS
│  └─ getAuthenticatedAccount(accountId) → Optional<Account>
│     Returns only if account valid
│
└─ SESSION
   └─ logout(accountId)
      Placeholder for token blacklist
```

## AuthorizationService - Permission & Role Checks

```
┌─ ROLE CHECKS (boolean)
│  ├─ hasRole(accountId, roleName) → boolean
│  ├─ hasAnyRole(accountId, roles...) → boolean
│  ├─ hasAllRoles(accountId, roles...) → boolean
│  ├─ isAdmin(accountId) → boolean
│  └─ isPassenger(accountId) → boolean
│
├─ PERMISSION CHECKS (boolean)
│  ├─ hasPermission(accountId, permissionName) → boolean
│  ├─ hasAnyPermission(accountId, permissions...) → boolean
│  └─ hasAllPermissions(accountId, permissions...) → boolean
│
├─ STRICT AUTHORIZATION (throws SecurityException)
│  ├─ requireRole(accountId, roleName)
│  ├─ requireAdmin(accountId)
│  ├─ requirePermission(accountId, permissionName)
│  ├─ requireAnyPermission(accountId, permissions...)
│  └─ requireAllPermissions(accountId, permissions...)
│
└─ INFO RETRIEVAL
   ├─ getAccountRoles(accountId) → Set<String>
   └─ getAccountPermissions(accountId) → Set<String>
```

## RoleService - Role Management

```
┌─ CREATE & FIND
│  ├─ createRole(name, description) → Role
│  ├─ findByName(name) → Optional<Role>
│  ├─ findById(roleId) → Optional<Role>
│  ├─ findByIdWithPermissions(roleId) → Optional<Role>
│  │  Eager loads permissions
│  │
│  ├─ getAllRoles() → List<Role>
│  ├─ getAllRolesWithPermissions() → List<Role>
│  └─ findRolesByNames(names) → Set<Role>
│
├─ PERMISSION MANAGEMENT
│  ├─ addPermissionToRole(roleId, permId) → Role
│  ├─ addPermissionToRoleByName(roleName, permName) → Role
│  ├─ removePermissionFromRole(roleId, permId) → Role
│  ├─ removePermissionFromRoleByName(roleName, permName) → Role
│  ├─ getRolePermissions(roleId) → Set<Permission>
│  └─ getRolePermissionsByName(roleName) → Set<Permission>
│
└─ UTILITIES
   ├─ roleExists(roleName) → boolean
   └─ updateRoleDescription(roleId, description) → Role
```

## PermissionService - Permission Management

```
┌─ CREATE & FIND
│  ├─ createPermission(name, description) → Permission
│  ├─ findByName(name) → Optional<Permission>
│  ├─ findById(permId) → Optional<Permission>
│  ├─ getAllPermissions() → List<Permission>
│  └─ findPermissionsByNames(names) → Set<Permission>
│
├─ SEARCH
│  └─ searchByKeyword(keyword) → List<Permission>
│     SQL LIKE query
│
└─ UTILITIES
   ├─ permissionExists(name) → boolean
   ├─ updatePermissionDescription(permId, description) → Permission
   └─ getPermissionCount() → long
```

---

## Common API Patterns

### Pattern: Check Permission (Soft)
```java
if (authService.hasPermission(accountId, "TICKET_VIEW")) {
    // Allow action
    return ticketService.getAll();
} else {
    // Deny silently
    return ResponseEntity.status(403).build();
}
```

### Pattern: Check Permission (Strict)
```java
try {
    authService.requirePermission(accountId, "TICKET_CREATE");
    return ResponseEntity.ok(ticketService.create(request));
} catch (SecurityException e) {
    return ResponseEntity.status(403).body(e.getMessage());
}
```

### Pattern: Check Multiple Permissions (Any)
```java
if (authService.hasAnyPermission(accountId, 
    "TICKET_CREATE", "TICKET_EDIT", "TICKET_DELETE")) {
    // User can manage tickets in some way
    return ticketService.getManagementPage();
}
```

### Pattern: Check Multiple Permissions (All)
```java
if (authService.hasAllPermissions(accountId,
    "PAYMENT_PROCESS", "TICKET_CREATE", "REFUND_ISSUE")) {
    // User can complete complex operation
    return processComplexOrder(request);
}
```

### Pattern: Admin-Only Endpoint
```java
@PostMapping("/api/admin/accounts")
public ResponseEntity<?> manageAccounts() {
    String accountId = getCurrentAccountId();
    
    try {
        authService.requireAdmin(accountId);
        // Admin operations
    } catch (SecurityException e) {
        return ResponseEntity.status(403).build();
    }
}
```

---

## Return Type Summary

| Return Type | Service Methods | Usage |
|------------|-----------------|-------|
| `Account` | 15 | Account operations |
| `Role` | 10 | Role operations |
| `Permission` | 3 | Permission operations |
| `Optional<Account>` | 4 | Lookups with nullability |
| `Optional<Role>` | 3 | Role lookups |
| `Optional<Permission>` | 2 | Permission lookups |
| `Set<Role>` | 2 | Collections |
| `Set<Permission>` | 4 | Collections |
| `Set<String>` | 2 | Info retrieval |
| `List<Account>` | 2 | Account lists |
| `List<Role>` | 2 | Role lists |
| `List<Permission>` | 2 | Permission lists |
| `boolean` | 18 | Checks & validations |
| `long` | 4 | Counts |
| `void` | 2 | Session ops |

---

## Parameter Patterns

| Parameter | Used In | Description |
|-----------|---------|-------------|
| `accountId` | Most methods | String (UUID) |
| `roleName` | Most Role/Auth methods | String (PASSENGER, APP_ADMIN) |
| `permissionName` | Most Permission/Auth methods | String (TICKET_VIEW, ACCOUNT_MANAGE) |
| `email` | Account methods | String |
| `password` | Account/Auth methods | String (hashed in storage) |
| `roleId` | Some Role methods | Integer |
| `permissionId` | Some Permission methods | Integer |

---

## Exception Reference

| Exception | Thrown By | When |
|-----------|-----------|------|
| `IllegalArgumentException` | Account/Role/Permission Service | Input validation, duplicate detection |
| `SecurityException` | AuthorizationService (strict) | Permission/Role denied |
| `RuntimeException` | AccountService | PASSENGER role not found (system error) |

---

## Transaction Scope

All methods marked with `@Transactional`:
- ✅ `registerAccount()`
- ✅ `verifyEmail()`
- ✅ `updatePassword()`
- ✅ `resetPassword()`
- ✅ `assignRole()`
- ✅ `removeRole()`
- ✅ `updateStatus()`
- ✅ `createRole()`
- ✅ `addPermissionToRole()`
- ✅ `removePermissionFromRole()`
- ✅ `updateRoleDescription()`
- ✅ `createPermission()`
- ✅ `updatePermissionDescription()`
- ✅ `logout()` (placeholder)

All others are read-only, **no transaction overhead**.

---

## Dependency Graph

```
┌──────────────────────────────┐
│  AccountService              │
│  @Autowired                  │
│  - AccountRepository         │
│  - RoleRepository            │
│  - PasswordEncoder           │
└──────────────────────────────┘
          ↑
          │ uses
          │
┌──────────────────────────────┐
│  AuthenticationService       │
│  @Autowired                  │
│  - AccountService            │
│  - AuthorizationService      │
└──────────────────────────────┘
          ↑
          │ uses
          │
┌──────────────────────────────┐
│  AuthorizationService        │
│  @Autowired                  │
│  - AccountService            │
│  - RoleService               │
│  - PermissionService         │
└──────────────────────────────┘
```

---

## Ready for:
✅ REST Controller creation
✅ Security annotations (@PreAuthorize, @PostAuthorize)
✅ JWT implementation
✅ API documentation (Swagger/OpenAPI)
✅ Integration testing
✅ Performance optimization
