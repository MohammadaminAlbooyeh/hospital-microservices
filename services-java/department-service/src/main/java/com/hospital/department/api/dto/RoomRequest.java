package com.hospital.department.api.dto;

import jakarta.validation.constraints.NotBlank;

public record RoomRequest(
        @NotBlank String roomNumber,
        @NotBlank String roomType
) {
}
