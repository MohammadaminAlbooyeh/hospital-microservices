package com.hospital.doctor.api.dto;

import com.hospital.doctor.domain.model.DayOfWeekEnum;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record ScheduleRequest(
        @NotNull DayOfWeekEnum dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        Integer maxAppointments
) {
}
