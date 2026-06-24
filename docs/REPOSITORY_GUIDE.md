# JPA Repositories - Documentation

## Overview
Three core JPA repositories have been created to manage authentication and authorization entities.

---

## 1. AccountRepository

**Location**: `com.ticketapp.repository.AccountRepository`

**Extends**: `JpaRepository<Account, String>`

### Methods

#### Finder Methods (Auto-generated)
- `findByEmail(String email): Optional<Account>` - Find account by email
- `findByPhoneNumber(String phoneNumber): Optional<Account>` - Find account by phone number
- `findByPersonalId(String personalId): Optional<Account>` - Find account by personal ID (CCCD)

#### Existence Checks
- `existsByEmail(String email): boolean` - Check if email exists
- `existsByPhoneNumber(String phoneNumber): boolean` - Check if phone number exists
- `existsByPersonalId(String personalId): boolean` - Check if personal ID exists

#### Custom Query Methods
- `findActiveByEmail(String email): Optional<Account>` - Find active account by email (isActive = true)
- `findAllVerifiedAndActive(): List<Account>` - Get all accounts that are email verified and active
- `countActiveAccounts(): long` - Count all active accounts
- `countUnverifiedAccounts(): long` - Count accounts with unverified emails
- `findAccountsRequiringPasswordChange(): List<Account>` - Get accounts flagged to change password

### Usage Example
```java
@Autowired
private AccountRepository accountRepository;

// Find by email
Optional<Account> account = accountRepository.findByEmail("user@example.com");

// Check if active
Optional<Account> active = accountRepository.findActiveByEmail("user@example.com");

// Get statistics
long activeCount = accountRepository.countActiveAccounts();
```

---

## 2. RoleRepository

**Location**: `com.ticketapp.repository.RoleRepository`

**Extends**: `JpaRepository<Role, Integer>`

### Methods

#### Finder Methods (Auto-generated)
- `findByName(String name): Optional<Role>` - Find role by name

#### Existence Checks
- `existsByName(String name): boolean` - Check if role exists by name

#### Custom Query Methods
- `findByNameIn(List<String> roleNames): Set<Role>` - Find multiple roles by names
- `findByIdWithPermissions(Integer id): Optional<Role>` - Find role with eager-loaded permissions
- `findAllWithPermissions(): List<Role>` - Get all roles with their permissions eagerly loaded

### Usage Example
```java
@Autowired
private RoleRepository roleRepository;

// Find role by name
Optional<Role> passengerRole = roleRepository.findByName("PASSENGER");

// Get multiple roles with permissions
Set<Role> roles = roleRepository.findByNameIn(Arrays.asList("PASSENGER", "APP_ADMIN"));

// Get all roles with permissions
List<Role> allRoles = roleRepository.findAllWithPermissions();
```

---

## 3. PermissionRepository

**Location**: `com.ticketapp.repository.PermissionRepository`

**Extends**: `JpaRepository<Permission, Integer>`

### Methods

#### Finder Methods (Auto-generated)
- `findByName(String name): Optional<Permission>` - Find permission by name

#### Existence Checks
- `existsByName(String name): boolean` - Check if permission exists by name

#### Custom Query Methods
- `findByNameIn(List<String> permissionNames): Set<Permission>` - Find multiple permissions by names
- `searchByKeyword(String keyword): List<Permission>` - Search permissions by keyword

### Usage Example
```java
@Autowired
private PermissionRepository permissionRepository;

// Find permission by name
Optional<Permission> perm = permissionRepository.findByName("TICKET_VIEW");

// Get multiple permissions
Set<Permission> perms = permissionRepository.findByNameIn(
    Arrays.asList("TICKET_VIEW", "TICKET_CREATE")
);

// Search
List<Permission> results = permissionRepository.searchByKeyword("TICKET");
```

---

## Key Features

### 1. **Automatic CRUD Operations**
All repositories inherit automatic implementations for:
- `save(Entity)` - Create/Update
- `findAll()` - Get all records
- `findById(ID)` - Get by primary key
- `deleteById(ID)` - Delete
- `delete(Entity)` - Delete entity
- `count()` - Count records

### 2. **Query Methods**
- Generated from method names (Spring Data magic)
- Custom `@Query` annotations for complex queries
- Named parameters with `@Param`

### 3. **Eager Loading**
- Used in RoleRepository to prevent N+1 query problems
- `LEFT JOIN FETCH` loads related permissions

### 4. **Type Safety**
- Generic types ensure compile-time safety
- IDE autocomplete for all methods

### 5. **Transaction Management**
- Inherited from `JpaRepository`
- Automatically transactional where needed

---

## Database Queries Generated

### AccountRepository
```sql
-- findByEmail
SELECT * FROM accounts WHERE email = ?

-- findActiveByEmail
SELECT * FROM accounts WHERE email = ? AND is_active = true

-- countActiveAccounts
SELECT COUNT(*) FROM accounts WHERE is_active = true

-- findAccountsRequiringPasswordChange
SELECT * FROM accounts WHERE must_change_password = true
```

### RoleRepository
```sql
-- findByName
SELECT * FROM roles WHERE name = ?

-- findByIdWithPermissions (with joins)
SELECT r.*, p.* FROM roles r 
LEFT JOIN role_permissions rp ON r.id = rp.role_id
LEFT JOIN permissions p ON rp.permission_id = p.id
WHERE r.id = ?

-- findByNameIn
SELECT * FROM roles WHERE name IN (?, ?)
```

### PermissionRepository
```sql
-- findByName
SELECT * FROM permissions WHERE name = ?

-- searchByKeyword
SELECT * FROM permissions WHERE name LIKE ?
```

---

## Integration Points

These repositories are designed to be injected into:
1. **Service Layer** - Business logic and authorization
2. **Controllers** - REST endpoints
3. **Security Components** - Authentication/Authorization filters

### Service Layer Example
```java
@Service
public class AccountService {
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    public Account findByEmail(String email) {
        return accountRepository.findActiveByEmail(email)
            .orElseThrow(() -> new AccountNotFoundException());
    }
    
    public void assignRoleToAccount(String accountId, String roleName) {
        Account account = accountRepository.findById(accountId).orElseThrow();
        Role role = roleRepository.findByName(roleName).orElseThrow();
        account.getRoles().add(role);
        accountRepository.save(account);
    }
}
```

---

## Next Steps

1. ✅ Repositories created
2. ⏳ Create Service layer with business logic
3. ⏳ Add authorization checks using permissions
4. ⏳ Create REST Controllers
5. ⏳ Create DTOs for API payloads
6. ⏳ Implement security/authentication

---

## Notes

- All repositories use `@Repository` annotation for component scanning
- Primary keys: Account (String/UUID), Role (Integer), Permission (Integer)
- Queries use JPQL (Java Persistence Query Language)
- Thread-safe and ready for production use
- Spring Data JPA handles connection pooling and transaction management
