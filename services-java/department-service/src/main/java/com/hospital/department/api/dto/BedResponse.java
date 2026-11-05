package com.hospital.department.api.dto;

import com.hospital.department.domain.model.BedStatus;

import java.util.UUID;

public record BedResponse(
        UUID id,
        UUID roomId,
        String bedNumber,
        BedStatus status
) {
}
