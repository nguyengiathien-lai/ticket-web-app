# JPA Repositories Quick Reference

## Repository Methods Summary

### AccountRepository
| Method | Return Type | Purpose |
|--------|------------|---------|
| `findByEmail(String)` | `Optional<Account>` | Find by email |
| `findByPhoneNumber(String)` | `Optional<Account>` | Find by phone |
| `findByPersonalId(String)` | `Optional<Account>` | Find by personal ID |
| `existsByEmail(String)` | `boolean` | Check email exists |
| `existsByPhoneNumber(String)` | `boolean` | Check phone exists |
| `existsByPersonalId(String)` | `boolean` | Check personal ID exists |
| `findActiveByEmail(String)` | `Optional<Account>` | Find active account by email |
| `findAllVerifiedAndActive()` | `List<Account>` | Get verified + active accounts |
| `countActiveAccounts()` | `long` | Count active accounts |
| `countUnverifiedAccounts()` | `long` | Count unverified accounts |
| `findAccountsRequiringPasswordChange()` | `List<Account>` | Get accounts needing password change |

### RoleRepository
| Method | Return Type | Purpose |
|--------|------------|---------|
| `findByName(String)` | `Optional<Role>` | Find by role name |
| `existsByName(String)` | `boolean` | Check if role exists |
| `findByNameIn(List<String>)` | `Set<Role>` | Find multiple roles by names |
| `findByIdWithPermissions(Integer)` | `Optional<Role>` | Find role with eager-loaded permissions |
| `findAllWithPermissions()` | `List<Role>` | Get all roles with permissions |

### PermissionRepository
| Method | Return Type | Purpose |
|--------|------------|---------|
| `findByName(String)` | `Optional<Permission>` | Find by permission name |
| `existsByName(String)` | `boolean` | Check if permission exists |
| `findByNameIn(List<String>)` | `Set<Permission>` | Find multiple permissions by names |
| `searchByKeyword(String)` | `List<Permission>` | Search permissions by keyword |

---

## Inherited CRUD Methods (from JpaRepository)

All repositories automatically inherit:

| Method | Purpose |
|--------|---------|
| `save(Entity)` | Create or update |
| `saveAll(Iterable)` | Batch save |
| `findById(ID)` | Get by ID |
| `findAll()` | Get all records |
| `findAll(Pageable)` | Get paginated results |
| `count()` | Count total records |
| `delete(Entity)` | Delete entity |
| `deleteById(ID)` | Delete by ID |
| `deleteAll()` | Delete all records |
| `exists(ID)` | Check if exists |
| `flush()` | Flush changes to DB |

---

## Usage Patterns

### Pattern 1: Find and Check
```java
if (accountRepository.existsByEmail("test@example.com")) {
    // Email already registered
}
```

### Pattern 2: Optional Handling
```java
accountRepository.findByEmail("user@example.com")
    .ifPresent(account -> {
        // Do something with account
    });
```

### Pattern 3: Or Throw
```java
Account account = accountRepository.findActiveByEmail(email)
    .orElseThrow(() -> new AccountNotFoundException());
```

### Pattern 4: Multiple Records
```java
Set<Role> roles = roleRepository.findByNameIn(
    Arrays.asList("PASSENGER", "APP_ADMIN")
);
```

### Pattern 5: Save
```java
Account newAccount = new Account();
newAccount.setId(UUID.randomUUID().toString());
newAccount.setEmail("user@example.com");
accountRepository.save(newAccount);
```

---

## Dependency Injection

### In Service Classes
```java
@Service
public class AuthService {
    
    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PermissionRepository permissionRepository;
    
    // Use repositories here
}
```

### In Controllers
```java
@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    
    @Autowired
    private AccountRepository accountRepository;
    
    @GetMapping("/{email}")
    public ResponseEntity<Account> getByEmail(@PathVariable String email) {
        return accountRepository.findByEmail(email)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
```

---

## Files Created

```
backend/src/main/java/com/ticketapp/repository/
├── AccountRepository.java      (User accounts)
├── RoleRepository.java         (Roles with permissions)
└── PermissionRepository.java   (Permissions)
```

---

## Next: Service Layer

Create corresponding Service classes:
- `AccountService`
- `RoleService`
- `PermissionService`

Ready to proceed? 🚀
