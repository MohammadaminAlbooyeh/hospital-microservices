package com.hospital.department.api.dto;

import jakarta.validation.constraints.NotBlank;

public record DepartmentRequest(
        @NotBlank String name,
        String floor,
        String phone
) {
}
