package com.hospital.prescription.domain.repository;

import com.hospital.prescription.domain.model.Prescription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrescriptionRepository {

    Prescription save(Prescription prescription);

    Optional<Prescription> findById(UUID id);

    List<Prescription> search(UUID patientId, UUID encounterId);
}
