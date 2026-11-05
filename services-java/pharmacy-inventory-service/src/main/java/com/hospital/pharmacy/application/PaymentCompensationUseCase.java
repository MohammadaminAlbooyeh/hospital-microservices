package com.hospital.pharmacy.application;

import com.hospital.pharmacy.domain.service.PharmacyDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentCompensationUseCase {

    private final PharmacyDomainService pharmacyDomainService;

    @Transactional
    public int finalizePayment(Collection<String> references) {
        int finalized = pharmacyDomainService.finalizeDispenses(references);
        log.debug("PaymentCompleted finalized {} dispense record(s)", finalized);
        return finalized;
    }

    @Transactional
    public int compensateFailedPayment(Collection<String> references) {
        int reversed = pharmacyDomainService.reverseDispenses(references);
        log.info("PaymentFailed compensated {} dispense record(s)", reversed);
        return reversed;
    }
}
