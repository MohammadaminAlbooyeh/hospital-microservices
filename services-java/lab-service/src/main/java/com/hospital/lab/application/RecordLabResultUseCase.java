package com.hospital.lab.application;

import com.hospital.lab.api.dto.RecordLabResultRequest;
import com.hospital.lab.api.mapper.LabMapper;
import com.hospital.lab.domain.model.LabOrder;
import com.hospital.lab.domain.model.LabResult;
import com.hospital.lab.domain.service.LabOrderDomainService;
import com.hospital.lab.infrastructure.messaging.LabEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecordLabResultUseCase {

    private final LabOrderDomainService labOrderDomainService;
    private final LabEventPublisher eventPublisher;

    @Transactional
    public LabResult execute(UUID orderId, RecordLabResultRequest request) {
        LabOrder order = labOrderDomainService.get(orderId);
        LabResult result = labOrderDomainService.recordResult(orderId, LabMapper.toDraft(request));
        eventPublisher.labResultReady(order, result);
        return result;
    }
}
