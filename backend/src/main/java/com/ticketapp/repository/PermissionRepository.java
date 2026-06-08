package com.ticketapp.repository;

import com.ticketapp.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Integer> {

    Optional<Permission> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT p FROM Permission p WHERE p.name IN :permissionNames")
    Set<Permission> findByNameIn(@Param("permissionNames") java.util.List<String> permissionNames);

    @Query("SELECT p FROM Permission p WHERE p.name LIKE %:keyword%")
    java.util.List<Permission> searchByKeyword(@Param("keyword") String keyword);
}
