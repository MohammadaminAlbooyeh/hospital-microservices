package com.hospital.lab.application;

import com.hospital.lab.domain.model.LabOrder;
import com.hospital.lab.domain.model.Specimen;
import com.hospital.lab.domain.service.LabOrderDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CollectSpecimenUseCase {

    private final LabOrderDomainService labOrderDomainService;

    @Transactional
    public CollectedSpecimen execute(UUID orderId, String collectedBy) {
        Specimen specimen = labOrderDomainService.collect(orderId, collectedBy);
        LabOrder order = labOrderDomainService.get(orderId);
        return new CollectedSpecimen(specimen, order);
    }

    public record CollectedSpecimen(Specimen specimen, LabOrder order) {
    }
}
