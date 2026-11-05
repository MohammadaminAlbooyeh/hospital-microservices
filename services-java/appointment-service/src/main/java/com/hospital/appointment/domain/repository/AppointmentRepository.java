package com.hospital.appointment.domain.repository;

import com.hospital.appointment.domain.model.Appointment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppointmentRepository {

    Appointment save(Appointment appointment);

    Optional<Appointment> findById(UUID id);

    List<Appointment> findByPatientId(UUID patientId);

    List<Appointment> findByDoctorId(UUID doctorId);

    List<Appointment> findByDoctorIdAndSlotStartBetween(UUID doctorId, Instant from, Instant to);

    boolean existsActiveByDoctorIdAndSlotStart(UUID doctorId, Instant slotStart);
}
