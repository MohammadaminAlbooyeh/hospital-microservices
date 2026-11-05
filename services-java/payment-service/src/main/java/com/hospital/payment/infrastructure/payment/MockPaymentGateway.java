package com.hospital.payment.infrastructure.payment;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@Slf4j
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public GatewayResult charge(GatewayChargeRequest request) {
        if (request.simulateFailure()) {
            log.info("Mock gateway declining payment {} because simulateFailure=true", request.paymentId());
            return GatewayResult.failure("Simulated payment failure");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Mock gateway declining payment {} because amount is not positive", request.paymentId());
            return GatewayResult.failure("Payment amount must be greater than zero");
        }
        String reference = "MOCK-" + UUID.randomUUID();
        log.info("Mock gateway approved payment {} with reference {}", request.paymentId(), reference);
        return GatewayResult.success(reference);
    }
}
