package com.hospital.payment.infrastructure.payment;

public record GatewayResult(
        boolean success,
        String gatewayReference,
        String failureReason
) {

    public static GatewayResult success(String gatewayReference) {
        return new GatewayResult(true, gatewayReference, null);
    }

    public static GatewayResult failure(String failureReason) {
        return new GatewayResult(false, null, failureReason);
    }
}
