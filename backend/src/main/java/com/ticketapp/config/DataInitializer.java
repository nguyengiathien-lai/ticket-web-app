package com.ticketapp.config;

import com.ticketapp.entity.Role;
import com.ticketapp.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        createRoleIfMissing("PASSENGER", "Default passenger account role");
        createRoleIfMissing("APP_ADMIN", "System administrator account role");
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
