package com.hospital.appointment.infrastructure.persistence;

import com.hospital.appointment.domain.model.Appointment;
import com.hospital.appointment.domain.repository.AppointmentRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface JpaAppointmentRepository extends JpaRepository<Appointment, UUID>, AppointmentRepository {

    @Override
    List<Appointment> findByPatientId(UUID patientId);

    @Override
    List<Appointment> findByDoctorId(UUID doctorId);

    @Override
    List<Appointment> findByDoctorIdAndSlotStartBetween(UUID doctorId, Instant from, Instant to);

    boolean existsByDoctorIdAndSlotStartAndStatusNot(UUID doctorId, Instant slotStart,
                                                       com.hospital.appointment.domain.model.AppointmentStatus status);

    @Override
    default boolean existsActiveByDoctorIdAndSlotStart(UUID doctorId, Instant slotStart) {
        return existsByDoctorIdAndSlotStartAndStatusNot(doctorId, slotStart,
                com.hospital.appointment.domain.model.AppointmentStatus.CANCELLED);
    }
}
