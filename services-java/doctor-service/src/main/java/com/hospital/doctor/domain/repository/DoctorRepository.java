package com.hospital.doctor.domain.repository;

import com.hospital.doctor.domain.model.Doctor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DoctorRepository {

    Doctor save(Doctor doctor);

    Optional<Doctor> findById(UUID id);

    List<Doctor> findAll();

    List<Doctor> findBySpeciality(String speciality);

    boolean existsByEmail(String email);

    boolean existsByLicenseNumber(String licenseNumber);

    void delete(Doctor doctor);
}
