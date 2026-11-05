package com.hospital.encounter.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelEncounterRequest(
        @NotBlank String reason
) {
}
