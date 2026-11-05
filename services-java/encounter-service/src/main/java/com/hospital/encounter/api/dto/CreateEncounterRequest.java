package com.hospital.encounter.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateEncounterRequest(
        @NotNull UUID patientId,
        UUID doctorId,
        UUID departmentId,
        String chiefComplaint
) {
}
