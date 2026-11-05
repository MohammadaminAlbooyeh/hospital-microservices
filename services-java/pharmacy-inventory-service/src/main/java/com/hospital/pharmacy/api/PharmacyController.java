package com.hospital.pharmacy.api;

import com.hospital.pharmacy.api.dto.DispenseRequest;
import com.hospital.pharmacy.api.dto.DispenseResponse;
import com.hospital.pharmacy.api.dto.DrugResponse;
import com.hospital.pharmacy.api.dto.StockUpsertRequest;
import com.hospital.pharmacy.api.mapper.PharmacyMapper;
import com.hospital.pharmacy.application.DispenseMedicationUseCase;
import com.hospital.pharmacy.application.ReceiveStockUseCase;
import com.hospital.pharmacy.domain.service.PharmacyDomainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pharmacy")
@RequiredArgsConstructor
public class PharmacyController {

    private final ReceiveStockUseCase receiveStockUseCase;
    private final DispenseMedicationUseCase dispenseMedicationUseCase;
    private final PharmacyDomainService pharmacyDomainService;

    @PostMapping("/stock")
    public DrugResponse receiveStock(@Valid @RequestBody StockUpsertRequest request) {
        return PharmacyMapper.toResponse(receiveStockUseCase.execute(request));
    }

    @GetMapping("/stock")
    public List<DrugResponse> listStock() {
        return pharmacyDomainService.listDrugs().stream()
                .map(PharmacyMapper::toResponse)
                .toList();
    }

    @GetMapping("/stock/low")
    public List<DrugResponse> lowStock() {
        return pharmacyDomainService.lowStock().stream()
                .map(PharmacyMapper::toResponse)
                .toList();
    }

    @GetMapping("/stock/expiring")
    public List<DrugResponse> expiring(@RequestParam(defaultValue = "30") int days) {
        return pharmacyDomainService.expiringWithin(days).stream()
                .map(PharmacyMapper::toResponse)
                .toList();
    }

    @GetMapping("/stock/{code}")
    public DrugResponse getStock(@PathVariable String code) {
        return PharmacyMapper.toResponse(pharmacyDomainService.getDrug(code));
    }

    @PostMapping("/dispense")
    public DispenseResponse dispense(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                     @Valid @RequestBody DispenseRequest request) {
        return PharmacyMapper.toResponse(dispenseMedicationUseCase.execute(idempotencyKey, request));
    }

    @GetMapping("/dispense/{id}")
    public DispenseResponse getDispense(@PathVariable UUID id) {
        return PharmacyMapper.toResponse(pharmacyDomainService.getDispense(id));
    }
}
