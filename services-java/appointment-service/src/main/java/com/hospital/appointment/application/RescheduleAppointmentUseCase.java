package com.hospital.appointment.application;

import com.hospital.appointment.domain.model.Appointment;
import com.hospital.appointment.domain.service.AppointmentDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RescheduleAppointmentUseCase {

    private final AppointmentDomainService appointmentDomainService;

    @Transactional
    public Appointment execute(UUID id, Instant slotStart, Instant slotEnd) {
        return appointmentDomainService.reschedule(id, slotStart, slotEnd);
    }
}
