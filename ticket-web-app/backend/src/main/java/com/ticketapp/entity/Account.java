package com.ticketapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "accounts")
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @Column(length = 36)
    private String id; // UUID

    @Column(nullable = false, unique = true, length = 50)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(name = "passenger_code", unique = true, length = 50)
    private String passengerCode;

    @Column(name = "date_of_birth")
    private java.time.LocalDate dateOfBirth;

    @Column(length = 20)
    private String gender;

    @Column(length = 10)
    private String phoneNumber;

    @Column(length = 200)
    private String address;

    @Column(nullable = true, length = 12)
    private String personalId;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Boolean isEmailVerified = false;

    @Column(nullable = false)
    private Boolean mustChangePassword = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private java.time.LocalDateTime updatedAt;

    @ManyToMany(fetch = jakarta.persistence.FetchType.EAGER)
    @JoinTable(
        name = "account_roles",
        joinColumns = @JoinColumn(name = "account_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<Role> roles = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}
