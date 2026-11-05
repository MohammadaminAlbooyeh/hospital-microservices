package com.hospital.payment.domain.repository;

import com.hospital.payment.domain.model.Payment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findById(UUID id);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findByInvoiceId(UUID invoiceId);

    List<Payment> findAll();
}
