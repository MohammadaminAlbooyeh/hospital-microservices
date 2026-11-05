package com.hospital.auth.domain.model;

import com.hospital.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "user_accounts", indexes = {
        @Index(name = "idx_user_accounts_email", columnList = "email")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAccount extends BaseEntity {

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 128)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 128)
    private String lastName;

    @Column(length = 64)
    private String phone;

    @Column(nullable = false, length = 255)
    private String roles;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(name = "otp_code", length = 16)
    private String otpCode;

    @Column(name = "otp_expires_at")
    private Instant otpExpiresAt;

    public List<String> roleList() {
        if (roles == null || roles.isBlank()) {
            return List.of();
        }
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(String::toUpperCase)
                .toList();
    }

    public boolean hasRole(Role role) {
        return roleList().contains(role.name());
    }
}
