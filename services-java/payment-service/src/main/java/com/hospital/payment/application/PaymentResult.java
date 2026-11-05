package com.hospital.payment.application;

import com.hospital.payment.domain.model.Payment;

public record PaymentResult(Payment payment, boolean created) {
}
