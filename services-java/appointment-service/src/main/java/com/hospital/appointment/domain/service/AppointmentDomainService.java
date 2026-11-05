package com.hospital.appointment.domain.service;

import com.hospital.appointment.domain.model.Appointment;
import com.hospital.appointment.domain.model.AppointmentStatus;
import com.hospital.appointment.domain.repository.AppointmentRepository;
import com.hospital.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

/**
 * Owns slot booking, rescheduling and cancellation. Double-booking is
 * guarded in two layers: an explicit existence check before insert, and a
 * partial unique index (doctor_id, slot_start) as a last line of defence
 * against two concurrent requests racing past the check (surfaced here as
 * a 409 Conflict instead of a raw SQL error).
 */
@Service
@RequiredArgsConstructor
public class AppointmentDomainService {

    private final AppointmentRepository appointmentRepository;

    @Transactional
    public Appointment book(UUID patientId, UUID doctorId, UUID departmentId, Instant slotStart, Instant slotEnd,
                             String reason) {
        if (slotEnd != null && !slotEnd.isAfter(slotStart)) {
            throw ApiException.badRequest("slotEnd must be after slotStart");
        }
        ensureSlotFree(doctorId, slotStart);
        Appointment appointment = Appointment.builder()
                .patientId(patientId)
                .doctorId(doctorId)
                .departmentId(departmentId)
                .slotStart(slotStart)
                .slotEnd(slotEnd)
                .reason(reason)
                .status(AppointmentStatus.BOOKED)
                .build();
        try {
            return appointmentRepository.save(appointment);
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("Doctor " + doctorId + " already has an appointment at " + slotStart);
        }
    }

    @Transactional
    public Appointment reschedule(UUID id, Instant newSlotStart, Instant newSlotEnd) {
        Appointment appointment = get(id);
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw ApiException.conflict("Cannot reschedule a cancelled appointment");
        }
        if (newSlotEnd != null && !newSlotEnd.isAfter(newSlotStart)) {
            throw ApiException.badRequest("slotEnd must be after slotStart");
        }
        if (!newSlotStart.equals(appointment.getSlotStart())) {
            ensureSlotFree(appointment.getDoctorId(), newSlotStart);
        }
        appointment.setSlotStart(newSlotStart);
        appointment.setSlotEnd(newSlotEnd);
        appointment.setStatus(AppointmentStatus.RESCHEDULED);
        try {
            return appointmentRepository.save(appointment);
        } catch (DataIntegrityViolationException ex) {
            throw ApiException.conflict("Doctor " + appointment.getDoctorId()
                    + " already has an appointment at " + newSlotStart);
        }
    }

    @Transactional
    public Appointment cancel(UUID id, String reason) {
        Appointment appointment = get(id);
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            return appointment;
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledAt(Instant.now());
        if (reason != null && !reason.isBlank()) {
            appointment.setReason(reason);
        }
        return appointmentRepository.save(appointment);
    }

    @Transactional(readOnly = true)
    public Appointment get(UUID id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Appointment " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<Appointment> findByPatient(UUID patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    @Transactional(readOnly = true)
    public List<Appointment> findByDoctor(UUID doctorId) {
        return appointmentRepository.findByDoctorId(doctorId);
    }

    @Transactional(readOnly = true)
    public List<Appointment> findByDoctorAndDate(UUID doctorId, LocalDate date) {
        Instant from = date.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return appointmentRepository.findByDoctorIdAndSlotStartBetween(doctorId, from, to);
    }

    private void ensureSlotFree(UUID doctorId, Instant slotStart) {
        if (appointmentRepository.existsActiveByDoctorIdAndSlotStart(doctorId, slotStart)) {
            throw ApiException.conflict("Doctor " + doctorId + " already has an appointment at " + slotStart);
        }
    }
}
