package com.hospital.auth.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone,
        List<String> roles,
        boolean enabled,
        boolean emailVerified,
        Instant createdAt
) {
}
