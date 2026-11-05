package com.hospital.appointment.api.dto;

import com.hospital.appointment.domain.model.AppointmentStatus;

import java.time.Instant;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID patientId,
        UUID doctorId,
        UUID departmentId,
        Instant slotStart,
        Instant slotEnd,
        AppointmentStatus status,
        String reason,
        Instant cancelledAt,
        long version,
        Instant createdAt,
        Instant updatedAt
) {
}
