package com.hospital.appointment.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record RescheduleAppointmentRequest(
        @NotNull Instant slotStart,
        Instant slotEnd
) {
}
