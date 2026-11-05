package com.hospital.appointment.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record BookAppointmentRequest(
        @NotNull UUID patientId,
        @NotNull UUID doctorId,
        UUID departmentId,
        @NotNull Instant slotStart,
        Instant slotEnd,
        String reason
) {
}
