package com.hospital.doctor.api.dto;

import java.time.Instant;
import java.util.UUID;

public record DoctorResponse(
        UUID id,
        String firstName,
        String lastName,
        String speciality,
        String email,
        String phone,
        String licenseNumber,
        UUID departmentId,
        Integer dailyCapacity,
        boolean active,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
}
