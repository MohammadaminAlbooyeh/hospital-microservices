package com.hospital.prescription.api.dto;

import com.hospital.prescription.domain.model.PrescriptionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PrescriptionResponse(
        UUID id,
        UUID patientId,
        UUID encounterId,
        UUID doctorId,
        PrescriptionStatus status,
        String notes,
        Instant issuedAt,
        List<ItemResponse> items,
        List<InteractionWarning> interactionWarnings,
        long version,
        Instant createdAt,
        Instant updatedAt
) {

    public record ItemResponse(
            UUID id,
            String drugCode,
            String drugName,
            String dose,
            String frequency,
            Integer durationDays,
            String instructions
    ) {
    }

    public record InteractionWarning(
            String drugA,
            String drugB,
            String severity,
            String description
    ) {
    }
}
