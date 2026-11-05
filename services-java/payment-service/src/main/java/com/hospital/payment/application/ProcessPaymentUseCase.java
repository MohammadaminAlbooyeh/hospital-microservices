package com.hospital.payment.application;

import com.hospital.payment.domain.model.Payment;
import com.hospital.payment.domain.model.PaymentStatus;
import com.hospital.payment.domain.repository.PaymentRepository;
import com.hospital.payment.infrastructure.messaging.PaymentEventPublisher;
import com.hospital.payment.infrastructure.payment.GatewayChargeRequest;
import com.hospital.payment.infrastructure.payment.GatewayResult;
import com.hospital.payment.infrastructure.payment.PaymentGateway;
import com.hospital.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProcessPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final PaymentEventPublisher eventPublisher;

    @Transactional
    public PaymentResult execute(ChargePaymentCommand command) {
        if (command.idempotencyKey() == null || command.idempotencyKey().isBlank()) {
            throw ApiException.badRequest("Idempotency-Key header is required");
        }
        if (command.invoiceId() == null) {
            throw ApiException.badRequest("invoiceId is required");
        }
        if (command.amount() == null) {
            throw ApiException.badRequest("amount is required");
        }
        if (command.currency() == null || command.currency().isBlank()) {
            throw ApiException.badRequest("currency is required");
        }
        if (command.method() == null) {
            throw ApiException.badRequest("method is required");
        }

        Optional<Payment> replay = paymentRepository.findByIdempotencyKey(command.idempotencyKey());
        if (replay.isPresent()) {
            return new PaymentResult(replay.get(), false);
        }

        Payment payment = Payment.builder()
                .invoiceId(command.invoiceId())
                .patientId(command.patientId())
                .amount(command.amount())
                .currency(command.currency().toUpperCase(Locale.ROOT))
                .method(command.method())
                .status(PaymentStatus.PENDING)
                .idempotencyKey(command.idempotencyKey())
                .build();
        payment = paymentRepository.save(payment);

        GatewayResult gatewayResult = paymentGateway.charge(new GatewayChargeRequest(
                payment.getId(),
                payment.getInvoiceId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getMethod(),
                command.simulateFailure()));

        Instant now = Instant.now();
        if (gatewayResult.success()) {
            payment.setStatus(PaymentStatus.SUCCEEDED);
            payment.setGatewayReference(gatewayResult.gatewayReference());
            payment.setFailureReason(null);
            payment.setProcessedAt(now);
            payment = paymentRepository.save(payment);
            eventPublisher.paymentCompleted(payment);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(gatewayResult.failureReason() == null
                    ? "Payment declined" : gatewayResult.failureReason());
            payment.setProcessedAt(now);
            payment = paymentRepository.save(payment);
            eventPublisher.paymentFailed(payment);
        }

        return new PaymentResult(payment, true);
    }
}
