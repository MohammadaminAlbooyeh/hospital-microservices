package com.hospital.payment.unit;

import com.hospital.payment.application.ChargePaymentCommand;
import com.hospital.payment.application.PaymentResult;
import com.hospital.payment.application.ProcessPaymentUseCase;
import com.hospital.payment.domain.model.Payment;
import com.hospital.payment.domain.model.PaymentMethod;
import com.hospital.payment.domain.model.PaymentStatus;
import com.hospital.payment.domain.repository.PaymentRepository;
import com.hospital.payment.infrastructure.messaging.PaymentEventPublisher;
import com.hospital.payment.infrastructure.payment.GatewayResult;
import com.hospital.payment.infrastructure.payment.PaymentGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessPaymentUseCaseTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private PaymentEventPublisher eventPublisher;

    @InjectMocks
    private ProcessPaymentUseCase processPaymentUseCase;

    @Test
    void replaysStoredPaymentForSameIdempotencyKey() {
        Payment existing = Payment.builder()
                .invoiceId(UUID.randomUUID())
                .amount(new BigDecimal("125.00"))
                .currency("USD")
                .method(PaymentMethod.CARD)
                .status(PaymentStatus.SUCCEEDED)
                .idempotencyKey("idem-1")
                .gatewayReference("MOCK-existing")
                .build();
        when(paymentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.of(existing));

        PaymentResult result = processPaymentUseCase.execute(command(false));

        assertThat(result.created()).isFalse();
        assertThat(result.payment()).isSameAs(existing);
        verifyNoInteractions(paymentGateway, eventPublisher);
        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void persistsFailedPaymentAndPublishesPaymentFailed() {
        when(paymentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentGateway.charge(any())).thenReturn(GatewayResult.failure("declined by gateway"));

        PaymentResult result = processPaymentUseCase.execute(command(false));

        assertThat(result.created()).isTrue();
        assertThat(result.payment().getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.payment().getFailureReason()).isEqualTo("declined by gateway");
        assertThat(result.payment().getProcessedAt()).isNotNull();
        assertThat(result.payment().getGatewayReference()).isNull();
        verify(eventPublisher).paymentFailed(result.payment());
        verify(eventPublisher, never()).paymentCompleted(any());
    }

    @Test
    void persistsSucceededPaymentAndPublishesPaymentCompleted() {
        when(paymentRepository.findByIdempotencyKey("idem-1")).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentGateway.charge(any())).thenReturn(GatewayResult.success("MOCK-approved"));

        PaymentResult result = processPaymentUseCase.execute(command(false));

        assertThat(result.created()).isTrue();
        assertThat(result.payment().getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(result.payment().getGatewayReference()).isEqualTo("MOCK-approved");
        assertThat(result.payment().getFailureReason()).isNull();
        assertThat(result.payment().getProcessedAt()).isNotNull();
        verify(eventPublisher).paymentCompleted(result.payment());
        verify(eventPublisher, never()).paymentFailed(any());
    }

    private ChargePaymentCommand command(boolean simulateFailure) {
        return new ChargePaymentCommand(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("125.00"),
                "usd",
                PaymentMethod.CARD,
                simulateFailure,
                "idem-1");
    }
}
