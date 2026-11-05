package com.hospital.department.api.dto;

import com.hospital.department.domain.model.EquipmentStatus;

import java.util.UUID;

public record EquipmentResponse(
        UUID id,
        UUID departmentId,
        String name,
        Integer quantity,
        EquipmentStatus status
) {
}
