package com.hospital.payment.infrastructure.persistence;

import com.hospital.payment.domain.model.Payment;
import com.hospital.payment.domain.repository.PaymentRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaPaymentRepository extends JpaRepository<Payment, UUID>, PaymentRepository {
}
