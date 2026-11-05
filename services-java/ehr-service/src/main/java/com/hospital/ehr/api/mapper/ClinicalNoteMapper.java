package com.hospital.ehr.api.mapper;

import com.hospital.ehr.api.dto.ClinicalNoteResponse;
import com.hospital.ehr.domain.model.ClinicalNote;

public final class ClinicalNoteMapper {

    private ClinicalNoteMapper() {
    }

    public static ClinicalNoteResponse toResponse(ClinicalNote note) {
        return new ClinicalNoteResponse(
                note.getId(),
                note.getPatientId(),
                note.getEncounterId(),
                note.getAuthorId(),
                note.getClinicalVersion(),
                note.getContent(),
                note.diagnosisList(),
                note.getSupersedesId(),
                note.getVersion(),
                note.getCreatedAt(),
                note.getUpdatedAt());
    }
}
