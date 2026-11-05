package com.hospital.doctor.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record LeaveRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String reason
) {
}
