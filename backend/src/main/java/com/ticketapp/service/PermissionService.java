package com.ticketapp.service;

import com.ticketapp.entity.Permission;
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
public class PermissionService {

    @Autowired
    private PermissionRepository permissionRepository;

    /**
     * Create a new permission (admin only)
     */
    @Transactional
    public Permission createPermission(String name, String description) {
        // Check if permission already exists
        if (permissionRepository.existsByName(name)) {
            throw new IllegalArgumentException("Permission already exists: " + name);
        }

        Permission permission = new Permission();
        permission.setName(name);
        permission.setDescription(description);
        permission.setCreatedAt(LocalDateTime.now());
        permission.setUpdatedAt(LocalDateTime.now());

        Permission savedPermission = permissionRepository.save(permission);
        log.info("Permission created: {}", name);

        return savedPermission;
    }

    /**
     * Find permission by name
     */
    public Optional<Permission> findByName(String name) {
        return permissionRepository.findByName(name);
    }

    /**
     * Find permission by ID
     */
    public Optional<Permission> findById(Integer permissionId) {
        return permissionRepository.findById(permissionId);
    }

    /**
     * Get all permissions
     */
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    /**
     * Find permissions by names
     */
    public Set<Permission> findPermissionsByNames(List<String> permissionNames) {
        return permissionRepository.findByNameIn(permissionNames);
    }

    /**
     * Search permissions by keyword
     */
    public List<Permission> searchByKeyword(String keyword) {
        return permissionRepository.searchByKeyword(keyword);
    }

    /**
     * Check if permission exists
     */
    public boolean permissionExists(String name) {
        return permissionRepository.existsByName(name);
    }

    /**
     * Update permission description
     */
    @Transactional
    public Permission updatePermissionDescription(Integer permissionId, String description) {
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new IllegalArgumentException("Permission not found"));

        permission.setDescription(description);
        permission.setUpdatedAt(LocalDateTime.now());

        Permission updated = permissionRepository.save(permission);
        log.info("Permission {} description updated", permissionId);

        return updated;
    }

    /**
     * Get total permission count
     */
    public long getPermissionCount() {
        return permissionRepository.count();
    }
}
