package com.hospital.pharmacy.api.mapper;

import com.hospital.pharmacy.api.dto.DispenseResponse;
import com.hospital.pharmacy.api.dto.DrugResponse;
import com.hospital.pharmacy.api.dto.StockUpsertRequest;
import com.hospital.pharmacy.domain.model.DispenseRecord;
import com.hospital.pharmacy.domain.model.Drug;

public final class PharmacyMapper {

    private PharmacyMapper() {
    }

    public static Drug toEntity(StockUpsertRequest request) {
        return Drug.builder()
                .code(request.code())
                .name(request.name())
                .form(request.form())
                .unit(request.unit())
                .unitPrice(request.unitPrice())
                .reorderLevel(request.reorderLevel())
                .expiryDate(request.expiryDate())
                .build();
    }

    public static DrugResponse toResponse(Drug drug) {
        return new DrugResponse(
                drug.getId(),
                drug.getCode(),
                drug.getName(),
                drug.getForm(),
                drug.getUnit(),
                drug.getUnitPrice(),
                drug.getQuantityOnHand(),
                drug.getReorderLevel(),
                drug.getExpiryDate(),
                drug.getVersion(),
                drug.getCreatedAt(),
                drug.getUpdatedAt());
    }

    public static DispenseResponse toResponse(DispenseRecord record) {
        return new DispenseResponse(
                record.getId(),
                record.getIdempotencyKey(),
                record.getPatientId(),
                record.getEncounterId(),
                record.getPrescriptionId(),
                record.getDrugCode(),
                record.getQuantity(),
                record.getStatus(),
                record.getFinalizedAt(),
                record.getVersion(),
                record.getCreatedAt(),
                record.getUpdatedAt());
    }
}
