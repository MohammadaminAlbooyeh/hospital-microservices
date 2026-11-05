package com.hospital.ehr.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ClinicalNoteResponse(
        UUID id,
        UUID patientId,
        UUID encounterId,
        UUID authorId,
        int version,
        String content,
        List<String> diagnoses,
        UUID supersedesId,
        long entityVersion,
        Instant createdAt,
        Instant updatedAt
) {
}
