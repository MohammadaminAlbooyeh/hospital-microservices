package com.hospital.ehr.infrastructure.persistence;

import com.hospital.ehr.domain.model.ClinicalNote;
import com.hospital.ehr.domain.repository.ClinicalNoteRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaClinicalNoteRepository extends JpaRepository<ClinicalNote, UUID>, ClinicalNoteRepository {
}
