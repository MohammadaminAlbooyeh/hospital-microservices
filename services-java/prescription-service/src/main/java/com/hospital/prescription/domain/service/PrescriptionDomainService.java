package com.hospital.prescription.domain.service;

import com.hospital.prescription.domain.model.Prescription;
import com.hospital.prescription.domain.model.PrescriptionStatus;
import com.hospital.prescription.domain.repository.PrescriptionRepository;
import com.hospital.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrescriptionDomainService {

    private final PrescriptionRepository prescriptionRepository;

    @Transactional(readOnly = true)
    public Prescription get(UUID id) {
        return prescriptionRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Prescription " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<Prescription> search(UUID patientId, UUID encounterId) {
        return prescriptionRepository.search(patientId, encounterId);
    }

    @Transactional
    public Prescription dispense(UUID id) {
        Prescription prescription = get(id);
        if (prescription.getStatus() != PrescriptionStatus.ISSUED) {
            throw ApiException.conflict("Only ISSUED prescriptions can be dispensed; current status is "
                    + prescription.getStatus());
        }
        prescription.setStatus(PrescriptionStatus.DISPENSED);
        return prescriptionRepository.save(prescription);
    }

    @Transactional
    public Prescription cancel(UUID id) {
        Prescription prescription = get(id);
        if (prescription.getStatus() != PrescriptionStatus.ISSUED) {
            throw ApiException.conflict("Only ISSUED prescriptions can be cancelled; current status is "
                    + prescription.getStatus());
        }
        prescription.setStatus(PrescriptionStatus.CANCELLED);
        return prescriptionRepository.save(prescription);
    }
}
