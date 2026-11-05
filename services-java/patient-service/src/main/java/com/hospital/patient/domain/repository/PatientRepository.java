package com.hospital.patient.domain.repository;

import com.hospital.patient.domain.model.Patient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatientRepository {

    Patient save(Patient patient);

    Optional<Patient> findById(UUID id);

    Optional<Patient> findByEmail(String email);

    List<Patient> findAll();

    boolean existsByEmail(String email);

    boolean existsByNationalId(String nationalId);

    void delete(Patient patient);
}
