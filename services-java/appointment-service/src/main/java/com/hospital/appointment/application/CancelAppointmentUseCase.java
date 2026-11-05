package com.hospital.appointment.application;

import com.hospital.appointment.domain.model.Appointment;
import com.hospital.appointment.domain.service.AppointmentDomainService;
import com.hospital.appointment.infrastructure.messaging.AppointmentEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CancelAppointmentUseCase {

    private final AppointmentDomainService appointmentDomainService;
    private final AppointmentEventPublisher eventPublisher;

    @Transactional
    public Appointment execute(UUID id, String reason) {
        Appointment appointment = appointmentDomainService.cancel(id, reason);
        eventPublisher.appointmentCancelled(appointment);
        return appointment;
    }
}
