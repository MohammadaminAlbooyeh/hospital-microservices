package com.hospital.lab.api;

import com.hospital.lab.api.dto.CollectSpecimenRequest;
import com.hospital.lab.api.dto.CollectSpecimenResponse;
import com.hospital.lab.api.dto.CreateLabOrderRequest;
import com.hospital.lab.api.dto.LabOrderResponse;
import com.hospital.lab.api.dto.LabResultResponse;
import com.hospital.lab.api.dto.RecordLabResultRequest;
import com.hospital.lab.api.mapper.LabMapper;
import com.hospital.lab.application.CancelLabOrderUseCase;
import com.hospital.lab.application.CollectSpecimenUseCase;
import com.hospital.lab.application.OrderLabTestUseCase;
import com.hospital.lab.application.RecordLabResultUseCase;
import com.hospital.lab.domain.model.LabOrder;
import com.hospital.lab.domain.model.LabOrderStatus;
import com.hospital.lab.domain.service.LabOrderDomainService;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/labs")
@RequiredArgsConstructor
public class LabController {

    private final OrderLabTestUseCase orderLabTestUseCase;
    private final CollectSpecimenUseCase collectSpecimenUseCase;
    private final RecordLabResultUseCase recordLabResultUseCase;
    private final CancelLabOrderUseCase cancelLabOrderUseCase;
    private final LabOrderDomainService labOrderDomainService;

    @PostMapping("/orders")
    public ResponseEntity<LabOrderResponse> createOrder(@Valid @RequestBody CreateLabOrderRequest request) {
        LabOrder order = orderLabTestUseCase.execute(request);
        return ResponseEntity.created(URI.create("/api/v1/labs/orders/" + order.getId()))
                .body(LabMapper.toResponse(order));
    }

    @GetMapping("/orders/{id}")
    public LabOrderResponse getOrder(@PathVariable UUID id) {
        return LabMapper.toResponse(labOrderDomainService.get(id));
    }

    @GetMapping("/orders")
    public List<LabOrderResponse> listOrders(@RequestParam(required = false) UUID patientId,
                                             @RequestParam(required = false) LabOrderStatus status) {
        return labOrderDomainService.search(patientId, status).stream()
                .map(LabMapper::toResponse)
                .toList();
    }

    @PostMapping("/orders/{id}/collect")
    public CollectSpecimenResponse collect(@PathVariable UUID id,
                                           @Valid @RequestBody CollectSpecimenRequest request) {
        CollectSpecimenUseCase.CollectedSpecimen collected =
                collectSpecimenUseCase.execute(id, request.collectedBy());
        return new CollectSpecimenResponse(
                LabMapper.toResponse(collected.order()),
                LabMapper.toResponse(collected.specimen()));
    }

    @PostMapping("/orders/{id}/result")
    public LabResultResponse recordResult(@PathVariable UUID id,
                                          @Valid @RequestBody RecordLabResultRequest request) {
        return LabMapper.toResponse(recordLabResultUseCase.execute(id, request));
    }

    @PostMapping("/orders/{id}/cancel")
    public LabOrderResponse cancel(@PathVariable UUID id) {
        return LabMapper.toResponse(cancelLabOrderUseCase.execute(id));
    }

    @GetMapping("/orders/{id}/result")
    public LabResultResponse getResult(@PathVariable UUID id) {
        return LabMapper.toResponse(labOrderDomainService.getResult(id));
    }
}
