package com.hospital.payment.api;

import com.hospital.payment.api.dto.CreatePaymentRequest;
import com.hospital.payment.api.dto.PaymentResponse;
import com.hospital.payment.api.dto.RefundRequest;
import com.hospital.payment.api.mapper.PaymentMapper;
import com.hospital.payment.application.ChargePaymentCommand;
import com.hospital.payment.application.PaymentResult;
import com.hospital.payment.application.ProcessPaymentUseCase;
import com.hospital.payment.application.RefundPaymentUseCase;
import com.hospital.payment.domain.model.Payment;
import com.hospital.payment.domain.service.PaymentDomainService;
import com.hospital.shared.exception.ApiException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final RefundPaymentUseCase refundPaymentUseCase;
    private final PaymentDomainService paymentDomainService;

    @PostMapping
    public ResponseEntity<PaymentResponse> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw ApiException.badRequest("Idempotency-Key header is required");
        }
        ChargePaymentCommand command = new ChargePaymentCommand(
                request.invoiceId(),
                request.patientId(),
                request.amount(),
                request.currency(),
                request.method(),
                Boolean.TRUE.equals(request.simulateFailure()),
                idempotencyKey);
        PaymentResult result = processPaymentUseCase.execute(command);
        PaymentResponse response = PaymentMapper.toResponse(result.payment());
        if (result.created()) {
            return ResponseEntity.created(URI.create("/api/v1/payments/" + result.payment().getId()))
                    .body(response);
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable UUID id) {
        return PaymentMapper.toResponse(paymentDomainService.get(id));
    }

    @GetMapping
    public List<PaymentResponse> list(@RequestParam(required = false) UUID invoiceId) {
        return paymentDomainService.list(invoiceId).stream()
                .map(PaymentMapper::toResponse)
                .toList();
    }

    @PostMapping("/{id}/refund")
    public PaymentResponse refund(@PathVariable UUID id,
                                  @Valid @RequestBody(required = false) RefundRequest request) {
        BigDecimal amount = request == null ? null : request.amount();
        Payment payment = refundPaymentUseCase.execute(id, amount);
        return PaymentMapper.toResponse(payment);
    }
}
