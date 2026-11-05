package com.hospital.department.api.dto;

import java.util.UUID;

public record RoomResponse(
        UUID id,
        UUID departmentId,
        String roomNumber,
        String roomType
) {
}
