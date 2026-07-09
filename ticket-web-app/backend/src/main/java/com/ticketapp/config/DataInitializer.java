package com.ticketapp.config;

import com.ticketapp.entity.Role;
import com.ticketapp.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(RoleRepository roleRepository, JdbcTemplate jdbcTemplate) {
        this.roleRepository = roleRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        relaxOptionalAccountColumns();
        ensureCascadeDeleteConstraints();
        createRoleIfMissing("PASSENGER", "Default passenger account role");
        createRoleIfMissing("APP_ADMIN", "System administrator account role");
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
}
