package com.hospital.encounter.api.dto;

import com.hospital.encounter.domain.model.EncounterState;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EncounterResponse(
        UUID id,
        UUID patientId,
        UUID doctorId,
        UUID departmentId,
        EncounterState state,
        String chiefComplaint,
        Instant checkedInAt,
        Instant startedAt,
        Instant completedAt,
        Instant cancelledAt,
        String cancelReason,
        List<String> diagnoses,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
}
