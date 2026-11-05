package com.hospital.doctor.domain.service;

import com.hospital.doctor.domain.model.Doctor;
import com.hospital.doctor.domain.model.DoctorLeave;
import com.hospital.doctor.domain.model.DoctorSchedule;
import com.hospital.doctor.domain.repository.DoctorLeaveRepository;
import com.hospital.doctor.domain.repository.DoctorRepository;
import com.hospital.doctor.domain.repository.DoctorScheduleRepository;
import com.hospital.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DoctorDomainService {

    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository doctorScheduleRepository;
    private final DoctorLeaveRepository doctorLeaveRepository;

    @Transactional
    public Doctor register(Doctor doctor) {
        if (doctorRepository.existsByEmail(doctor.getEmail())) {
            throw ApiException.conflict("A doctor with email " + doctor.getEmail() + " already exists");
        }
        if (doctorRepository.existsByLicenseNumber(doctor.getLicenseNumber())) {
            throw ApiException.conflict("A doctor with license number " + doctor.getLicenseNumber()
                    + " already exists");
        }
        if (doctor.getDailyCapacity() == null) {
            doctor.setDailyCapacity(20);
        }
        doctor.setActive(true);
        return doctorRepository.save(doctor);
    }

    @Transactional
    public Doctor update(UUID id, Doctor patch) {
        Doctor existing = get(id);
        existing.setFirstName(patch.getFirstName());
        existing.setLastName(patch.getLastName());
        existing.setSpeciality(patch.getSpeciality());
        existing.setPhone(patch.getPhone());
        existing.setDepartmentId(patch.getDepartmentId());
        if (patch.getDailyCapacity() != null) {
            existing.setDailyCapacity(patch.getDailyCapacity());
        }
        return doctorRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public Doctor get(UUID id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Doctor " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<Doctor> list() {
        return doctorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Doctor> findBySpeciality(String speciality) {
        return doctorRepository.findBySpeciality(speciality);
    }

    @Transactional
    public void delete(UUID id) {
        Doctor doctor = get(id);
        doctor.setActive(false);
        doctorRepository.save(doctor);
    }

    @Transactional
    public DoctorSchedule addSchedule(UUID doctorId, DoctorSchedule schedule) {
        get(doctorId);
        schedule.setDoctorId(doctorId);
        return doctorScheduleRepository.save(schedule);
    }

    @Transactional(readOnly = true)
    public List<DoctorSchedule> getSchedule(UUID doctorId) {
        return doctorScheduleRepository.findByDoctorId(doctorId);
    }

    @Transactional
    public DoctorLeave addLeave(UUID doctorId, DoctorLeave leave) {
        get(doctorId);
        if (leave.getEndDate().isBefore(leave.getStartDate())) {
            throw ApiException.badRequest("endDate must not be before startDate");
        }
        leave.setDoctorId(doctorId);
        return doctorLeaveRepository.save(leave);
    }

    @Transactional(readOnly = true)
    public List<DoctorLeave> getLeave(UUID doctorId) {
        return doctorLeaveRepository.findByDoctorId(doctorId);
    }

    /**
     * Availability for a specific date: whether the doctor is on leave, the
     * matching weekly schedule window (if any), and remaining daily capacity.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> availability(UUID doctorId, LocalDate date) {
        Doctor doctor = get(doctorId);
        List<DoctorLeave> leaves = doctorLeaveRepository.findByDoctorId(doctorId);
        boolean onLeave = leaves.stream().anyMatch(leave -> leave.covers(date));
        DayOfWeek javaDay = date.getDayOfWeek();
        List<DoctorSchedule> schedules = doctorScheduleRepository.findByDoctorId(doctorId);
        DoctorSchedule matching = schedules.stream()
                .filter(schedule -> schedule.getDayOfWeek().name().equals(javaDay.name()))
                .findFirst()
                .orElse(null);
        boolean available = !onLeave && matching != null && doctor.isActive();
        return Map.of(
                "doctorId", doctorId,
                "date", date,
                "onLeave", onLeave,
                "available", available,
                "schedule", matching,
                "dailyCapacity", doctor.getDailyCapacity());
    }
}
