package com.hospital.billing.api;

import com.hospital.billing.api.dto.CreateInvoiceRequest;
import com.hospital.billing.api.dto.InvoiceResponse;
import com.hospital.billing.api.mapper.InvoiceMapper;
import com.hospital.billing.application.CreateInvoiceUseCase;
import com.hospital.billing.application.RecalculateInvoiceUseCase;
import com.hospital.billing.application.VoidInvoiceUseCase;
import com.hospital.billing.domain.model.Invoice;
import com.hospital.billing.domain.model.InvoiceStatus;
import com.hospital.billing.domain.service.InvoiceDomainService;
import com.hospital.shared.exception.ApiException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/billing/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final CreateInvoiceUseCase createInvoiceUseCase;
    private final VoidInvoiceUseCase voidInvoiceUseCase;
    private final RecalculateInvoiceUseCase recalculateInvoiceUseCase;
    private final InvoiceDomainService invoiceDomainService;

    @PostMapping
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody CreateInvoiceRequest request) {
        Invoice invoice = createInvoiceUseCase.execute(request);
        return ResponseEntity.created(URI.create("/api/v1/billing/invoices/" + invoice.getId()))
                .body(InvoiceMapper.toResponse(invoice));
    }

    @GetMapping("/{id}")
    public InvoiceResponse get(@PathVariable UUID id) {
        return InvoiceMapper.toResponse(invoiceDomainService.get(id));
    }

    @GetMapping
    public List<InvoiceResponse> list(@RequestParam(required = false) UUID patientId,
                                      @RequestParam(required = false) String status) {
        return invoiceDomainService.list(patientId, parseStatus(status)).stream()
                .map(InvoiceMapper::toResponse)
                .toList();
    }

    @PostMapping("/{id}/void")
    public InvoiceResponse voidInvoice(@PathVariable UUID id) {
        return InvoiceMapper.toResponse(voidInvoiceUseCase.execute(id));
    }

    @PostMapping("/{id}/recalculate")
    public InvoiceResponse recalculate(@PathVariable UUID id) {
        return InvoiceMapper.toResponse(recalculateInvoiceUseCase.execute(id));
    }

    private InvoiceStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return InvoiceStatus.valueOf(status.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest("Unknown invoice status: " + status);
        }
    }
}
