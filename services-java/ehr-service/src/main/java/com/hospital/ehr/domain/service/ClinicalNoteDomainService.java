package com.hospital.ehr.domain.service;

import com.hospital.ehr.domain.model.ClinicalNote;
import com.hospital.ehr.domain.repository.ClinicalNoteRepository;
import com.hospital.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClinicalNoteDomainService {

    private final ClinicalNoteRepository clinicalNoteRepository;

    @Transactional(readOnly = true)
    public ClinicalNote current(UUID patientId) {
        return clinicalNoteRepository.findTopByPatientIdOrderByClinicalVersionDesc(patientId)
                .orElseThrow(() -> ApiException.notFound("No clinical notes found for patient " + patientId));
    }

    @Transactional(readOnly = true)
    public List<ClinicalNote> history(UUID patientId) {
        return clinicalNoteRepository.findByPatientIdOrderByClinicalVersionDesc(patientId);
    }

    @Transactional(readOnly = true)
    public ClinicalNote getVersion(UUID patientId, int clinicalVersion) {
        return clinicalNoteRepository.findByPatientIdAndClinicalVersion(patientId, clinicalVersion)
                .orElseThrow(() -> ApiException.notFound(
                        "Clinical note version " + clinicalVersion + " not found for patient " + patientId));
    }

    @Transactional(readOnly = true)
    public ClinicalNote get(UUID id) {
        return clinicalNoteRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Clinical note " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<String> diagnoses(UUID patientId) {
        return clinicalNoteRepository.findByPatientIdOrderByClinicalVersionDesc(patientId).stream()
                .flatMap(note -> note.diagnosisList().stream())
                .distinct()
                .sorted()
                .toList();
    }

    @Transactional
    public ClinicalNote createVersion(UUID patientId, UUID encounterId, UUID authorId,
                                      String content, List<String> diagnoses) {
        if (patientId == null) {
            throw ApiException.badRequest("patientId is required");
        }
        if (content == null || content.isBlank()) {
            throw ApiException.badRequest("Clinical note content must not be empty");
        }
        ClinicalNote previous = clinicalNoteRepository
                .findTopByPatientIdOrderByClinicalVersionDesc(patientId)
                .orElse(null);
        int nextVersion = previous == null ? 1 : previous.getClinicalVersion() + 1;
        ClinicalNote note = ClinicalNote.builder()
                .patientId(patientId)
                .encounterId(encounterId)
                .authorId(authorId)
                .clinicalVersion(nextVersion)
                .content(content.trim())
                .diagnoses(joinDiagnoses(diagnoses))
                .supersedesId(previous == null ? null : previous.getId())
                .build();
        return clinicalNoteRepository.saveAndFlush(note);
    }

    private String joinDiagnoses(List<String> diagnoses) {
        if (diagnoses == null || diagnoses.isEmpty()) {
            return null;
        }
        return diagnoses.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(String::trim)
                .distinct()
                .reduce((left, right) -> left + ", " + right)
                .orElse(null);
    }
}
