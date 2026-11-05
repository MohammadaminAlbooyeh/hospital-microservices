package com.hospital.department.api.dto;

import java.time.Instant;
import java.util.UUID;

public record DepartmentResponse(
        UUID id,
        String name,
        String floor,
        String phone,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
}
