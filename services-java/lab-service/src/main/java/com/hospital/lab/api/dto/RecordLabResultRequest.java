package com.hospital.lab.api.dto;

import jakarta.validation.constraints.NotBlank;

public record RecordLabResultRequest(
        @NotBlank String value,
        String unit,
        String referenceRange,
        boolean critical,
        String notes
) {
}
