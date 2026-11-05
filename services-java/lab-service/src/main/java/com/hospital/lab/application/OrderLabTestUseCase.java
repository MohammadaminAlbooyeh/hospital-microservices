package com.hospital.lab.application;

import com.hospital.lab.api.dto.CreateLabOrderRequest;
import com.hospital.lab.api.mapper.LabMapper;
import com.hospital.lab.domain.model.LabOrder;
import com.hospital.lab.domain.service.LabOrderDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderLabTestUseCase {

    private final LabOrderDomainService labOrderDomainService;

    @Transactional
    public LabOrder execute(CreateLabOrderRequest request) {
        return labOrderDomainService.order(LabMapper.toEntity(request));
    }
}
