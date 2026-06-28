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
        createRoleIfMissing("PASSENGER", "Default passenger account role");
        createRoleIfMissing("APP_ADMIN", "System administrator account role");
    }

    private void relaxOptionalAccountColumns() {
        jdbcTemplate.execute("ALTER TABLE accounts ALTER COLUMN personal_id DROP NOT NULL");
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
