package com.hospital.ehr.domain.repository;

import com.hospital.ehr.domain.model.ClinicalNote;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClinicalNoteRepository {

    ClinicalNote saveAndFlush(ClinicalNote note);

    Optional<ClinicalNote> findById(UUID id);

    Optional<ClinicalNote> findTopByPatientIdOrderByClinicalVersionDesc(UUID patientId);

    Optional<ClinicalNote> findByPatientIdAndClinicalVersion(UUID patientId, int clinicalVersion);

    List<ClinicalNote> findByPatientIdOrderByClinicalVersionDesc(UUID patientId);
}
