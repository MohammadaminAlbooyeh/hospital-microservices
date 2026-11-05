package com.hospital.payment.infrastructure.payment;

public interface PaymentGateway {

    GatewayResult charge(GatewayChargeRequest request);
}
