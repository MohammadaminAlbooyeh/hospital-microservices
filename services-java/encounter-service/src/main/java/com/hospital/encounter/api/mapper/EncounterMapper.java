package com.hospital.encounter.api.mapper;

import com.hospital.encounter.api.dto.CreateEncounterRequest;
import com.hospital.encounter.api.dto.EncounterResponse;
import com.hospital.encounter.domain.model.Encounter;

public final class EncounterMapper {

    private EncounterMapper() {
    }

    public static Encounter toEntity(CreateEncounterRequest request) {
        return Encounter.builder()
                .patientId(request.patientId())
                .doctorId(request.doctorId())
                .departmentId(request.departmentId())
                .chiefComplaint(request.chiefComplaint())
                .build();
    }

    public static EncounterResponse toResponse(Encounter encounter) {
        return new EncounterResponse(
                encounter.getId(),
                encounter.getPatientId(),
                encounter.getDoctorId(),
                encounter.getDepartmentId(),
                encounter.getState(),
                encounter.getChiefComplaint(),
                encounter.getCheckedInAt(),
                encounter.getStartedAt(),
                encounter.getCompletedAt(),
                encounter.getCancelledAt(),
                encounter.getCancelReason(),
                encounter.diagnosisList(),
                encounter.getVersion(),
                encounter.getCreatedAt(),
                encounter.getUpdatedAt());
    }
}
