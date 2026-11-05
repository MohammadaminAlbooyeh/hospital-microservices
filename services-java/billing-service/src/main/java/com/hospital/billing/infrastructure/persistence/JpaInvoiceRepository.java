package com.hospital.billing.infrastructure.persistence;

import com.hospital.billing.domain.model.Invoice;
import com.hospital.billing.domain.repository.InvoiceRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaInvoiceRepository extends JpaRepository<Invoice, UUID>, InvoiceRepository {
}
