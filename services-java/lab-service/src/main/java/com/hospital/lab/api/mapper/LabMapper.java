package com.hospital.lab.api.mapper;

import com.hospital.lab.api.dto.CreateLabOrderRequest;
import com.hospital.lab.api.dto.LabOrderResponse;
import com.hospital.lab.api.dto.LabResultResponse;
import com.hospital.lab.api.dto.RecordLabResultRequest;
import com.hospital.lab.api.dto.SpecimenResponse;
import com.hospital.lab.domain.model.LabOrder;
import com.hospital.lab.domain.model.LabResult;
import com.hospital.lab.domain.model.Specimen;

public final class LabMapper {

    private LabMapper() {
    }

    public static LabOrder toEntity(CreateLabOrderRequest request) {
        return LabOrder.builder()
                .patientId(request.patientId())
                .encounterId(request.encounterId())
                .testCode(request.testCode())
                .testName(request.testName())
                .priority(request.priority())
                .orderedBy(request.orderedBy())
                .notes(request.notes())
                .build();
    }

    public static LabResult toDraft(RecordLabResultRequest request) {
        return LabResult.builder()
                .value(request.value())
                .unit(request.unit())
                .referenceRange(request.referenceRange())
                .critical(request.critical())
                .notes(request.notes())
                .build();
    }

    public static LabOrderResponse toResponse(LabOrder order) {
        return new LabOrderResponse(
                order.getId(),
                order.getPatientId(),
                order.getEncounterId(),
                order.getTestCode(),
                order.getTestName(),
                order.getPriority(),
                order.getStatus(),
                order.getOrderedAt(),
                order.getOrderedBy(),
                order.getNotes(),
                order.getVersion(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    public static SpecimenResponse toResponse(Specimen specimen) {
        return new SpecimenResponse(
                specimen.getId(),
                specimen.getOrderId(),
                specimen.getBarcode(),
                specimen.getCollectedAt(),
                specimen.getCollectedBy(),
                specimen.getStatus());
    }

    public static LabResultResponse toResponse(LabResult result) {
        return new LabResultResponse(
                result.getId(),
                result.getOrderId(),
                result.getValue(),
                result.getUnit(),
                result.getReferenceRange(),
                result.isCritical(),
                result.getResultAt(),
                result.getNotes());
    }
}
