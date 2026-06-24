package com.ticketapp.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "otp_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OtpCode {

    @Id
    @Column(length = 36)
    private String id; // UUID

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(nullable = false, length = 50)
    private String type; // EMAIL_VERIFICATION, PASSWORD_RESET, LOGIN

    @Column(nullable = false)
    private Integer attemptCount = 0;

    @Column(nullable = false)
    private Integer maxAttempts = 5;

    @Column(nullable = false)
    private Boolean isUsed = false;

    @Column(name = "expires_at", nullable = false)
    private java.time.LocalDateTime expiresAt;

    @Column(name = "used_at")
    private java.time.LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        if (expiresAt == null) {
            expiresAt = java.time.LocalDateTime.now().plusMinutes(15);
        }
    }
}
