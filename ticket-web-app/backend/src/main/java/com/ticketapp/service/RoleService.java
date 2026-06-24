package com.ticketapp.service;

import com.ticketapp.entity.Role;
import com.ticketapp.entity.Permission;
import com.ticketapp.repository.RoleRepository;
import com.ticketapp.repository.PermissionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
public class RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    /**
     * Create a new role (admin only)
     */
    @Transactional
    public Role createRole(String name, String description) {
        // Check if role already exists
        if (roleRepository.existsByName(name)) {
            throw new IllegalArgumentException("Role already exists: " + name);
        }

        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());

        Role savedRole = roleRepository.save(role);
        log.info("Role created: {}", name);

        return savedRole;
    }

    /**
     * Find role by name
     */
    public Optional<Role> findByName(String name) {
        return roleRepository.findByName(name);
    }

    /**
     * Find role by ID
     */
    public Optional<Role> findById(Integer roleId) {
        return roleRepository.findById(roleId);
    }

    /**
     * Find role by ID with permissions (eager load)
     */
    public Optional<Role> findByIdWithPermissions(Integer roleId) {
        return roleRepository.findByIdWithPermissions(roleId);
    }

    /**
     * Get all roles with permissions
     */
    public List<Role> getAllRolesWithPermissions() {
        return roleRepository.findAllWithPermissions();
    }

    /**
     * Get all roles
     */
    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    /**
     * Find roles by names
     */
    public Set<Role> findRolesByNames(List<String> roleNames) {
        return roleRepository.findByNameIn(roleNames);
    }

    /**
     * Add permission to role
     */
    @Transactional
    public Role addPermissionToRole(Integer roleId, Integer permissionId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found"));

        if (role.getPermissions().contains(permission)) {
            log.warn("Permission {} already exists in role {}", permissionId, roleId);
            return role;
        }

        role.getPermissions().add(permission);
        role.setUpdatedAt(LocalDateTime.now());

        Role updated = roleRepository.save(role);
        log.info("Permission {} added to role {}", permissionId, roleId);

        return updated;
    }

    /**
     * Add permission to role by name
     */
    @Transactional
    public Role addPermissionToRoleByName(String roleName, String permissionName) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));

        Permission permission = permissionRepository.findByName(permissionName)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + permissionName));

        if (role.getPermissions().contains(permission)) {
            log.warn("Permission {} already exists in role {}", permissionName, roleName);
            return role;
        }

        role.getPermissions().add(permission);
        role.setUpdatedAt(LocalDateTime.now());

        Role updated = roleRepository.save(role);
        log.info("Permission {} added to role {}", permissionName, roleName);

        return updated;
    }

    /**
     * Remove permission from role
     */
    @Transactional
    public Role removePermissionFromRole(Integer roleId, Integer permissionId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found"));

        role.getPermissions().remove(permission);
        role.setUpdatedAt(LocalDateTime.now());

        Role updated = roleRepository.save(role);
        log.info("Permission {} removed from role {}", permissionId, roleId);

        return updated;
    }

    /**
     * Remove permission from role by name
     */
    @Transactional
    public Role removePermissionFromRoleByName(String roleName, String permissionName) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));

        Permission permission = permissionRepository.findByName(permissionName)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found: " + permissionName));

        role.getPermissions().remove(permission);
        role.setUpdatedAt(LocalDateTime.now());

        Role updated = roleRepository.save(role);
        log.info("Permission {} removed from role {}", permissionName, roleName);

        return updated;
    }

    /**
     * Get all permissions for a role
     */
    public Set<Permission> getRolePermissions(Integer roleId) {
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        return role.getPermissions();
    }

    /**
     * Get all permissions for a role by name
     */
    public Set<Permission> getRolePermissionsByName(String roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));

        return role.getPermissions();
    }

    /**
     * Check if role exists
     */
    public boolean roleExists(String roleName) {
        return roleRepository.existsByName(roleName);
    }

    /**
     * Update role description
     */
    @Transactional
    public Role updateRoleDescription(Integer roleId, String description) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("Role not found"));

        role.setDescription(description);
        role.setUpdatedAt(LocalDateTime.now());

        Role updated = roleRepository.save(role);
        log.info("Role {} description updated", roleId);

        return updated;
    }
}
