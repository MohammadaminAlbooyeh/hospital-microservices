package com.hospital.department.api.dto;

import com.hospital.department.domain.model.BedStatus;
import jakarta.validation.constraints.NotBlank;

public record BedRequest(
        @NotBlank String bedNumber,
        BedStatus status
) {
}
