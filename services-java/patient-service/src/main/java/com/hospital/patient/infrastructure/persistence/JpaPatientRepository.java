package com.hospital.patient.infrastructure.persistence;

import com.hospital.patient.domain.model.Patient;
import com.hospital.patient.domain.repository.PatientRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaPatientRepository extends JpaRepository<Patient, UUID>, PatientRepository {
}
