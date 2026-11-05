package com.hospital.prescription.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

public record IssuePrescriptionRequest(
        @NotNull UUID patientId,
        UUID encounterId,
        @NotNull UUID doctorId,
        String notes,
        @NotEmpty @Valid List<ItemRequest> items
) {

    public record ItemRequest(
            @NotBlank String drugCode,
            @NotBlank String drugName,
            String dose,
            String frequency,
            @Positive Integer durationDays,
            String instructions
    ) {
    }
}
