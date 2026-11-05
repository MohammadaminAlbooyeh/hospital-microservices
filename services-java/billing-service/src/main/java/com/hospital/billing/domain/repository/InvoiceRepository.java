package com.hospital.billing.domain.repository;

import com.hospital.billing.domain.model.Invoice;
import com.hospital.billing.domain.model.InvoiceStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository {

    Invoice save(Invoice invoice);

    Optional<Invoice> findById(UUID id);

    List<Invoice> findByPatientId(UUID patientId);

    List<Invoice> findByPatientIdAndStatus(UUID patientId, InvoiceStatus status);

    List<Invoice> findByStatus(InvoiceStatus status);

    List<Invoice> findAll();
}
