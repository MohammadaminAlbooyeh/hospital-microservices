package com.hospital.appointment.api.mapper;

import com.hospital.appointment.api.dto.AppointmentResponse;
import com.hospital.appointment.domain.model.Appointment;

public final class AppointmentMapper {

    private AppointmentMapper() {
    }

    public static AppointmentResponse toResponse(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatientId(),
                appointment.getDoctorId(),
                appointment.getDepartmentId(),
                appointment.getSlotStart(),
                appointment.getSlotEnd(),
                appointment.getStatus(),
                appointment.getReason(),
                appointment.getCancelledAt(),
                appointment.getVersion(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt());
    }
}
