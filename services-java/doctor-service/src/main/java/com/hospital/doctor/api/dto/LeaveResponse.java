package com.hospital.doctor.api.dto;

import java.time.LocalDate;
import java.util.UUID;

public record LeaveResponse(
        UUID id,
        UUID doctorId,
        LocalDate startDate,
        LocalDate endDate,
        String reason
) {
}
