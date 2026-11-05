package com.hospital.lab.api.dto;

import com.hospital.lab.domain.model.LabPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateLabOrderRequest(
        @NotNull UUID patientId,
        UUID encounterId,
        @NotBlank String testCode,
        @NotBlank String testName,
        @NotNull LabPriority priority,
        @NotBlank String orderedBy,
        String notes
) {
}
