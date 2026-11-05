package com.hospital.auth.domain.model;

import com.hospital.shared.exception.ApiException;

public enum Role {
    PATIENT,
    DOCTOR,
    NURSE,
    RECEPTIONIST,
    BILLING,
    ADMIN;

    public static Role from(String raw) {
        if (raw == null || raw.isBlank()) {
            return PATIENT;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Unsupported role: " + raw);
        }
    }
}
