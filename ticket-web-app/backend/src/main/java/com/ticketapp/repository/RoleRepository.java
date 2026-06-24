package com.ticketapp.repository;

import com.ticketapp.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    @Query("SELECT r FROM Role r WHERE r.name IN :roleNames")
    Set<Role> findByNameIn(@Param("roleNames") java.util.List<String> roleNames);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions WHERE r.id = :id")
    Optional<Role> findByIdWithPermissions(@Param("id") Integer id);

    @Query("SELECT r FROM Role r LEFT JOIN FETCH r.permissions")
    java.util.List<Role> findAllWithPermissions();
}
