package com.hospital.encounter.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CompleteEncounterRequest(
        @NotEmpty List<@NotBlank String> diagnoses
) {
}
