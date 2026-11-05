package com.hospital.lab.domain.service;

import com.hospital.lab.domain.model.LabOrder;
import com.hospital.lab.domain.model.LabOrderStatus;
import com.hospital.lab.domain.model.LabResult;
import com.hospital.lab.domain.model.Specimen;
import com.hospital.lab.domain.model.SpecimenStatus;
import com.hospital.lab.domain.repository.LabOrderRepository;
import com.hospital.lab.domain.repository.LabResultRepository;
import com.hospital.lab.domain.repository.SpecimenRepository;
import com.hospital.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LabOrderDomainService {

    private static final int BARCODE_ATTEMPTS = 5;

    private final LabOrderRepository labOrderRepository;
    private final SpecimenRepository specimenRepository;
    private final LabResultRepository labResultRepository;

    @Transactional
    public LabOrder order(LabOrder order) {
        if (order.getOrderedAt() == null) {
            order.setOrderedAt(Instant.now());
        }
        order.setStatus(LabOrderStatus.ORDERED);
        return labOrderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public LabOrder get(UUID id) {
        return labOrderRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Lab order " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<LabOrder> search(UUID patientId, LabOrderStatus status) {
        if (patientId != null && status != null) {
            return labOrderRepository.findByPatientIdAndStatusOrderByOrderedAtDesc(patientId, status);
        }
        if (patientId != null) {
            return labOrderRepository.findByPatientIdOrderByOrderedAtDesc(patientId);
        }
        if (status != null) {
            return labOrderRepository.findByStatusOrderByOrderedAtDesc(status);
        }
        return labOrderRepository.findAllByOrderByOrderedAtDesc();
    }

    @Transactional
    public Specimen collect(UUID orderId, String collectedBy) {
        LabOrder order = get(orderId);
        if (order.getStatus() != LabOrderStatus.ORDERED) {
            throw ApiException.conflict("Cannot collect specimen for lab order in status " + order.getStatus());
        }
        if (specimenRepository.findByOrderId(orderId).isPresent()) {
            throw ApiException.conflict("A specimen already exists for lab order " + orderId);
        }

        order.setStatus(LabOrderStatus.COLLECTED);
        labOrderRepository.save(order);

        Specimen specimen = Specimen.builder()
                .orderId(orderId)
                .barcode(nextBarcode())
                .collectedAt(Instant.now())
                .collectedBy(collectedBy)
                .status(SpecimenStatus.COLLECTED)
                .build();
        return specimenRepository.save(specimen);
    }

    @Transactional
    public LabResult recordResult(UUID orderId, LabResult draft) {
        LabOrder order = get(orderId);
        if (labResultRepository.existsByOrderId(orderId)) {
            throw ApiException.conflict("A result already exists for lab order " + orderId);
        }
        if (order.getStatus() != LabOrderStatus.COLLECTED && order.getStatus() != LabOrderStatus.IN_PROGRESS) {
            throw ApiException.conflict("Cannot record a result for lab order in status " + order.getStatus());
        }

        draft.setOrderId(orderId);
        draft.setResultAt(Instant.now());
        LabResult saved = labResultRepository.save(draft);

        order.setStatus(LabOrderStatus.RESULTED);
        labOrderRepository.save(order);
        return saved;
    }

    @Transactional
    public LabOrder cancel(UUID orderId) {
        LabOrder order = get(orderId);
        if (order.getStatus() == LabOrderStatus.RESULTED || order.getStatus() == LabOrderStatus.CANCELLED) {
            throw ApiException.conflict("Cannot cancel lab order in status " + order.getStatus());
        }
        order.setStatus(LabOrderStatus.CANCELLED);
        return labOrderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public LabResult getResult(UUID orderId) {
        return labResultRepository.findByOrderId(orderId)
                .orElseThrow(() -> ApiException.notFound("Result for lab order " + orderId + " not found"));
    }

    @Transactional(readOnly = true)
    public Specimen getSpecimen(UUID orderId) {
        return specimenRepository.findByOrderId(orderId)
                .orElseThrow(() -> ApiException.notFound("Specimen for lab order " + orderId + " not found"));
    }

    private String nextBarcode() {
        for (int attempt = 0; attempt < BARCODE_ATTEMPTS; attempt++) {
            String barcode = "LAB-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
            if (!specimenRepository.existsByBarcode(barcode)) {
                return barcode;
            }
        }
        throw ApiException.conflict("Unable to allocate a unique specimen barcode");
    }
}
