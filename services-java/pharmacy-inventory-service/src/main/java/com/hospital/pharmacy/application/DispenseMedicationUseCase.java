package com.hospital.pharmacy.application;

import com.hospital.pharmacy.api.dto.DispenseRequest;
import com.hospital.pharmacy.domain.model.DispenseRecord;
import com.hospital.pharmacy.domain.service.PharmacyDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DispenseMedicationUseCase {

    private final PharmacyDomainService pharmacyDomainService;

    @Transactional
    public DispenseRecord execute(String idempotencyKey, DispenseRequest request) {
        return pharmacyDomainService.dispense(
                request.patientId(),
                request.encounterId(),
                request.prescriptionId(),
                request.drugCode(),
                request.quantity(),
                idempotencyKey);
    }
}
