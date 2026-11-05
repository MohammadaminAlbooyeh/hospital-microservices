package com.hospital.department.api.dto;

import com.hospital.department.domain.model.EquipmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EquipmentRequest(
        @NotBlank String name,
        @NotNull Integer quantity,
        EquipmentStatus status
) {
}
