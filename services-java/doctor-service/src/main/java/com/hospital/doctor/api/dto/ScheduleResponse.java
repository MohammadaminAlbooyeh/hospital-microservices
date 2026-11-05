package com.hospital.doctor.api.dto;

import com.hospital.doctor.domain.model.DayOfWeekEnum;

import java.time.LocalTime;
import java.util.UUID;

public record ScheduleResponse(
        UUID id,
        UUID doctorId,
        DayOfWeekEnum dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        Integer maxAppointments
) {
}
