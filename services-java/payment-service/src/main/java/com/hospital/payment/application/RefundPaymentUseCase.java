package com.hospital.payment.application;

import com.hospital.payment.domain.model.Payment;
import com.hospital.payment.domain.service.PaymentDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefundPaymentUseCase {

    private final PaymentDomainService paymentDomainService;

    @Transactional
    public Payment execute(UUID paymentId, BigDecimal amount) {
        return paymentDomainService.refund(paymentId, amount);
    }
}
