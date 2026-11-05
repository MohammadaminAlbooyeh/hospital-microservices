package com.hospital.ehr.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateClinicalNoteRequest(
        @NotNull UUID patientId,
        UUID encounterId,
        UUID authorId,
        @NotBlank String content,
        List<String> diagnoses
) {
}
