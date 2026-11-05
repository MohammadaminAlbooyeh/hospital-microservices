package com.hospital.appointment.application;

import com.hospital.appointment.api.dto.BookAppointmentRequest;
import com.hospital.appointment.domain.model.Appointment;
import com.hospital.appointment.domain.service.AppointmentDomainService;
import com.hospital.appointment.infrastructure.messaging.AppointmentEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookAppointmentUseCase {

    private final AppointmentDomainService appointmentDomainService;
    private final AppointmentEventPublisher eventPublisher;

    @Transactional
    public Appointment execute(BookAppointmentRequest request) {
        Appointment appointment = appointmentDomainService.book(
                request.patientId(), request.doctorId(), request.departmentId(),
                request.slotStart(), request.slotEnd(), request.reason());
        eventPublisher.appointmentBooked(appointment);
        return appointment;
    }
}
