package com.hospital.prescription.application;

import com.hospital.prescription.domain.model.Prescription;
import com.hospital.prescription.domain.service.DrugInteraction;

import java.util.List;

public record IssuePrescriptionResult(
        Prescription prescription,
        List<DrugInteraction> warnings
) {
}
