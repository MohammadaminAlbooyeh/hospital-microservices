package com.hospital.prescription.api.mapper;

import com.hospital.prescription.api.dto.PrescriptionResponse;
import com.hospital.prescription.domain.model.Prescription;
import com.hospital.prescription.domain.model.PrescriptionItem;
import com.hospital.prescription.domain.service.DrugInteraction;

import java.util.List;

public final class PrescriptionMapper {

    private PrescriptionMapper() {
    }

    public static PrescriptionResponse toResponse(Prescription prescription) {
        return toResponse(prescription, List.of());
    }

    public static PrescriptionResponse toResponse(Prescription prescription,
                                                  List<DrugInteraction> warnings) {
        List<PrescriptionResponse.ItemResponse> items = prescription.getItems() == null
                ? List.of()
                : prescription.getItems().stream().map(PrescriptionMapper::toItemResponse).toList();
        List<PrescriptionResponse.InteractionWarning> interactionWarnings = warnings == null
                ? List.of()
                : warnings.stream()
                .map(warning -> new PrescriptionResponse.InteractionWarning(
                        warning.drugA(), warning.drugB(), warning.severity().name(), warning.description()))
                .toList();
        return new PrescriptionResponse(
                prescription.getId(),
                prescription.getPatientId(),
                prescription.getEncounterId(),
                prescription.getDoctorId(),
                prescription.getStatus(),
                prescription.getNotes(),
                prescription.getIssuedAt(),
                items,
                interactionWarnings,
                prescription.getVersion(),
                prescription.getCreatedAt(),
                prescription.getUpdatedAt());
    }

    private static PrescriptionResponse.ItemResponse toItemResponse(PrescriptionItem item) {
        return new PrescriptionResponse.ItemResponse(
                item.getId(),
                item.getDrugCode(),
                item.getDrugName(),
                item.getDose(),
                item.getFrequency(),
                item.getDurationDays(),
                item.getInstructions());
    }
}
