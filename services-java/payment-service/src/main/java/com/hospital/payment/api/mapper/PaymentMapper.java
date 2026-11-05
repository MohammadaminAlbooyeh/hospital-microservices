package com.hospital.payment.api.mapper;

import com.hospital.payment.api.dto.PaymentResponse;
import com.hospital.payment.domain.model.Payment;

public final class PaymentMapper {

    private PaymentMapper() {
    }

    public static PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getInvoiceId(),
                payment.getPatientId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getIdempotencyKey(),
                payment.getGatewayReference(),
                payment.getFailureReason(),
                payment.getProcessedAt(),
                payment.getRefundedAt(),
                payment.getRefundAmount(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
