package com.hospital.lab.application;

import com.hospital.lab.domain.model.LabOrder;
import com.hospital.lab.domain.service.LabOrderDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CancelLabOrderUseCase {

    private final LabOrderDomainService labOrderDomainService;

    @Transactional
    public LabOrder execute(UUID orderId) {
        return labOrderDomainService.cancel(orderId);
    }
}
