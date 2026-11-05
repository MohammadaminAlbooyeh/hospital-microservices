package com.hospital.pharmacy.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record DispenseRequest(
        @NotNull UUID patientId,
        UUID encounterId,
        UUID prescriptionId,
        @NotBlank String drugCode,
        @Positive int quantity
) {
}
