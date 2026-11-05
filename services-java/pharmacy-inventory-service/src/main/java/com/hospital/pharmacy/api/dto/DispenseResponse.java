package com.hospital.pharmacy.api.dto;

import com.hospital.pharmacy.domain.model.DispenseStatus;

import java.time.Instant;
import java.util.UUID;

public record DispenseResponse(
        UUID id,
        String idempotencyKey,
        UUID patientId,
        UUID encounterId,
        UUID prescriptionId,
        String drugCode,
        int quantity,
        DispenseStatus status,
        Instant finalizedAt,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
}
