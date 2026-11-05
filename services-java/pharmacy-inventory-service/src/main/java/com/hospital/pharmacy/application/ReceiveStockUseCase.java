package com.hospital.pharmacy.application;

import com.hospital.pharmacy.api.dto.StockUpsertRequest;
import com.hospital.pharmacy.api.mapper.PharmacyMapper;
import com.hospital.pharmacy.domain.model.Drug;
import com.hospital.pharmacy.domain.service.PharmacyDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReceiveStockUseCase {

    private final PharmacyDomainService pharmacyDomainService;

    @Transactional
    public Drug execute(StockUpsertRequest request) {
        return pharmacyDomainService.receive(PharmacyMapper.toEntity(request), request.quantity());
    }
}
