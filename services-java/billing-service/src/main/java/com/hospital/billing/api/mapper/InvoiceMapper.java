package com.hospital.billing.api.mapper;

import com.hospital.billing.api.dto.CreateInvoiceRequest;
import com.hospital.billing.api.dto.InvoiceLineRequest;
import com.hospital.billing.api.dto.InvoiceLineResponse;
import com.hospital.billing.api.dto.InvoiceResponse;
import com.hospital.billing.domain.model.Invoice;
import com.hospital.billing.domain.model.InvoiceLine;

import java.math.BigDecimal;

public final class InvoiceMapper {

    private InvoiceMapper() {
    }

    public static Invoice toNewEntity(CreateInvoiceRequest request) {
        Invoice invoice = Invoice.builder()
                .patientId(request.patientId())
                .encounterId(request.encounterId())
                .currency(request.currency())
                .insuranceCoveragePercent(request.insuranceCoveragePercent() == null
                        ? BigDecimal.ZERO : request.insuranceCoveragePercent())
                .build();
        for (InvoiceLineRequest lineRequest : request.lines()) {
            invoice.addLine(InvoiceLine.builder()
                    .category(lineRequest.category())
                    .description(lineRequest.description())
                    .quantity(lineRequest.quantity())
                    .unitPrice(lineRequest.unitPrice())
                    .build());
        }
        return invoice;
    }

    public static InvoiceResponse toResponse(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getId(),
                invoice.getEncounterId(),
                invoice.getPatientId(),
                invoice.getStatus(),
                invoice.getCurrency(),
                invoice.getSubtotal(),
                invoice.getInsurerShare(),
                invoice.getPatientShare(),
                invoice.getTotalAmount(),
                invoice.getInsuranceCoveragePercent(),
                invoice.getIssuedAt(),
                invoice.getPaidAt(),
                invoice.getFailureReason(),
                invoice.getLastPaymentId(),
                invoice.getCreatedAt(),
                invoice.getUpdatedAt(),
                invoice.getLines().stream().map(InvoiceMapper::toLineResponse).toList()
        );
    }

    private static InvoiceLineResponse toLineResponse(InvoiceLine line) {
        return new InvoiceLineResponse(
                line.getId(),
                line.getCategory(),
                line.getDescription(),
                line.getQuantity(),
                line.getUnitPrice(),
                line.getAmount()
        );
    }
}
