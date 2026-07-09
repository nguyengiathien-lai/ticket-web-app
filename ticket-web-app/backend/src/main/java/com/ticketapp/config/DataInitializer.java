package com.ticketapp.config;

import com.ticketapp.entity.Account;
import com.ticketapp.entity.Role;
import com.ticketapp.repository.AccountRepository;
import com.ticketapp.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final boolean seedDemoAccountsEnabled;
    private final String seedDemoAccountsPassword;

    public DataInitializer(
            AccountRepository accountRepository,
            RoleRepository roleRepository,
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.demo-accounts.enabled:false}") boolean seedDemoAccountsEnabled,
            @Value("${app.seed.demo-accounts.password:12345678}") String seedDemoAccountsPassword) {
        this.accountRepository = accountRepository;
        this.roleRepository = roleRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.seedDemoAccountsEnabled = seedDemoAccountsEnabled;
        this.seedDemoAccountsPassword = seedDemoAccountsPassword;
    }

    @Override
    public void run(String... args) {
        relaxOptionalAccountColumns();
        ensureCascadeDeleteConstraints();
        createRoleIfMissing("PASSENGER", "Default passenger account role");
        createRoleIfMissing("APP_ADMIN", "System administrator account role");
        if (seedDemoAccountsEnabled) {
            seedVerifiedAccounts();
        }
    }

    private void relaxOptionalAccountColumns() {
        jdbcTemplate.execute("ALTER TABLE accounts ALTER COLUMN personal_id DROP NOT NULL");
    }

    private void ensureCascadeDeleteConstraints() {
        recreateCascadeForeignKey("account_roles", "account_id", "accounts", "id", "fk_account_roles_account");
        recreateCascadeForeignKey("orders", "passenger_account_id", "accounts", "id", "fk_orders_passenger_account");
        recreateCascadeForeignKey("payments", "passenger_account_id", "accounts", "id", "fk_payments_passenger_account");
        recreateCascadeForeignKey("order_items", "order_id", "orders", "id", "fk_order_items_order");
    }

    private void recreateCascadeForeignKey(
            String tableName,
            String columnName,
            String referencedTableName,
            String referencedColumnName,
            String constraintName) {
        jdbcTemplate.execute("""
                DO $$
                DECLARE
                    existing_constraint_name text;
                BEGIN
                    IF to_regclass('public.%1$s') IS NULL OR to_regclass('public.%3$s') IS NULL THEN
                        RETURN;
                    END IF;

                    FOR existing_constraint_name IN
                        SELECT con.conname
                        FROM pg_constraint con
                        JOIN pg_class rel ON rel.oid = con.conrelid
                        JOIN pg_attribute attr ON attr.attrelid = con.conrelid
                            AND attr.attnum = ANY(con.conkey)
                        WHERE con.contype = 'f'
                            AND rel.relname = '%1$s'
                            AND attr.attname = '%2$s'
                            AND con.confrelid = 'public.%3$s'::regclass
                    LOOP
                        EXECUTE format('ALTER TABLE %%I DROP CONSTRAINT %%I', '%1$s', existing_constraint_name);
                    END LOOP;

                    IF NOT EXISTS (
                        SELECT 1
                        FROM pg_constraint
                        WHERE conname = '%5$s'
                            AND conrelid = 'public.%1$s'::regclass
                    ) THEN
                        EXECUTE 'ALTER TABLE %1$s ADD CONSTRAINT %5$s FOREIGN KEY (%2$s) REFERENCES %3$s(%4$s) ON DELETE CASCADE NOT VALID';
                    END IF;
                END $$;
                """.formatted(tableName, columnName, referencedTableName, referencedColumnName, constraintName));
    }

    private void createRoleIfMissing(String name, String description) {
        if (roleRepository.existsByName(name)) {
            return;
        }

        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        roleRepository.save(role);
    }

    private void seedVerifiedAccounts() {
        Role passengerRole = roleRepository.findByName("PASSENGER")
                .orElseThrow(() -> new IllegalStateException("PASSENGER role not found"));
        Role adminRole = roleRepository.findByName("APP_ADMIN")
                .orElseThrow(() -> new IllegalStateException("APP_ADMIN role not found"));

        createOrUpdateSeedAccount(
                "00000000-0000-0000-0000-000000000101",
                "nguyenvana@gmail.com",
                "Nguyen Van A",
                "PASSENGER-DEMO-001",
                LocalDate.of(1998, 5, 12),
                "MALE",
                "0901234567",
                "101 Nguyen Trai, Thanh Xuan, Ha Noi",
                "001098000101",
                Set.of(passengerRole));

        createOrUpdateSeedAccount(
                "00000000-0000-0000-0000-000000000102",
                "thienlai@gmail.com",
                "Thien Lai",
                "PASSENGER-DEMO-002",
                LocalDate.of(2000, 9, 20),
                "MALE",
                "0901234568",
                "Cat Linh, Dong Da, Ha Noi",
                "001100000102",
                Set.of(passengerRole));

        createOrUpdateSeedAccount(
                "00000000-0000-0000-0000-000000000201",
                "admin@transitpass.vn",
                "TransitPass Admin",
                "ADMIN-DEMO-001",
                LocalDate.of(1995, 1, 1),
                "OTHER",
                "0901234569",
                "TransitPass Operation Center",
                "001095000201",
                Set.of(adminRole));
    }

    private void createOrUpdateSeedAccount(
            String id,
            String email,
            String fullName,
            String passengerCode,
            LocalDate dateOfBirth,
            String gender,
            String phoneNumber,
            String address,
            String personalId,
            Set<Role> roles) {
        Account account = accountRepository.findByEmail(email).orElseGet(() -> {
            Account created = new Account();
            created.setId(id);
            created.setEmail(email);
            return created;
        });

        account.setPassword(passwordEncoder.encode(seedDemoAccountsPassword));
        account.setFullName(fullName);
        account.setPassengerCode(passengerCode);
        account.setDateOfBirth(dateOfBirth);
        account.setGender(gender);
        account.setPhoneNumber(phoneNumber);
        account.setAddress(address);
        account.setPersonalId(personalId);
        account.setIsActive(true);
        account.setIsEmailVerified(true);
        account.setMustChangePassword(false);
        roles.forEach(role -> {
            boolean alreadyAssigned = account.getRoles().stream()
                    .anyMatch(existing -> existing.getName().equals(role.getName()));
            if (!alreadyAssigned) {
                account.getRoles().add(role);
            }
        });

        accountRepository.save(account);
    }
}
