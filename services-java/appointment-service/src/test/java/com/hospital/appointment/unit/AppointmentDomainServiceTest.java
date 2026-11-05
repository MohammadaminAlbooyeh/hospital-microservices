package com.hospital.appointment.unit;

import com.hospital.appointment.domain.model.Appointment;
import com.hospital.appointment.domain.model.AppointmentStatus;
import com.hospital.appointment.domain.repository.AppointmentRepository;
import com.hospital.appointment.domain.service.AppointmentDomainService;
import com.hospital.shared.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentDomainServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private AppointmentDomainService appointmentDomainService;

    @Test
    void booksAnAppointmentWhenSlotIsFree() {
        UUID doctorId = UUID.randomUUID();
        Instant slotStart = Instant.now();
        when(appointmentRepository.existsActiveByDoctorIdAndSlotStart(doctorId, slotStart)).thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment appointment = appointmentDomainService.book(
                UUID.randomUUID(), doctorId, null, slotStart, null, "checkup");

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.BOOKED);
    }

    @Test
    void rejectsDoubleBookingOfTheSameDoctorSlot() {
        UUID doctorId = UUID.randomUUID();
        Instant slotStart = Instant.now();
        when(appointmentRepository.existsActiveByDoctorIdAndSlotStart(eq(doctorId), eq(slotStart)))
                .thenReturn(true);

        assertThatThrownBy(() -> appointmentDomainService.book(
                UUID.randomUUID(), doctorId, null, slotStart, null, "checkup"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already has an appointment");
    }
}
