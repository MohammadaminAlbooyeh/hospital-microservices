package com.hospital.lab.unit;

import com.hospital.lab.domain.model.LabOrder;
import com.hospital.lab.domain.model.LabOrderStatus;
import com.hospital.lab.domain.model.LabPriority;
import com.hospital.lab.domain.model.LabResult;
import com.hospital.lab.domain.model.Specimen;
import com.hospital.lab.domain.repository.LabOrderRepository;
import com.hospital.lab.domain.repository.LabResultRepository;
import com.hospital.lab.domain.repository.SpecimenRepository;
import com.hospital.lab.domain.service.LabOrderDomainService;
import com.hospital.shared.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabOrderDomainServiceTest {

    @Mock
    private LabOrderRepository labOrderRepository;

    @Mock
    private SpecimenRepository specimenRepository;

    @Mock
    private LabResultRepository labResultRepository;

    @InjectMocks
    private LabOrderDomainService labOrderDomainService;

    @Test
    void collectingSpecimenMovesOrderToCollectedAndCreatesBarcode() {
        LabOrder order = orderWithStatus(LabOrderStatus.ORDERED);
        when(labOrderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(labOrderRepository.save(any(LabOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(specimenRepository.save(any(Specimen.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Specimen specimen = labOrderDomainService.collect(order.getId(), "nurse-1");

        assertThat(order.getStatus()).isEqualTo(LabOrderStatus.COLLECTED);
        assertThat(specimen.getBarcode()).startsWith("LAB-");
        assertThat(specimen.getCollectedBy()).isEqualTo("nurse-1");
        assertThat(specimen.getOrderId()).isEqualTo(order.getId());
    }

    @Test
    void recordingResultMovesCollectedOrderToResulted() {
        LabOrder order = orderWithStatus(LabOrderStatus.COLLECTED);
        when(labOrderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(labResultRepository.existsByOrderId(order.getId())).thenReturn(false);
        when(labResultRepository.save(any(LabResult.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(labOrderRepository.save(any(LabOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LabResult result = labOrderDomainService.recordResult(order.getId(),
                LabResult.builder().value("5.4").unit("mmol/L").critical(false).build());

        assertThat(result.getResultAt()).isNotNull();
        assertThat(result.getOrderId()).isEqualTo(order.getId());
        assertThat(order.getStatus()).isEqualTo(LabOrderStatus.RESULTED);
    }

    @Test
    void rejectsResultForOrderThatHasNotBeenCollected() {
        LabOrder order = orderWithStatus(LabOrderStatus.ORDERED);
        when(labOrderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(labResultRepository.existsByOrderId(order.getId())).thenReturn(false);

        assertThatThrownBy(() -> labOrderDomainService.recordResult(order.getId(),
                LabResult.builder().value("1").build()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Cannot record a result");
    }

    @Test
    void rejectsDuplicateResultForOrder() {
        LabOrder order = orderWithStatus(LabOrderStatus.COLLECTED);
        when(labOrderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(labResultRepository.existsByOrderId(order.getId())).thenReturn(true);

        assertThatThrownBy(() -> labOrderDomainService.recordResult(order.getId(),
                LabResult.builder().value("1").build()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void rejectsCollectingAlreadyCollectedOrder() {
        LabOrder order = orderWithStatus(LabOrderStatus.COLLECTED);
        when(labOrderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> labOrderDomainService.collect(order.getId(), "nurse-1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Cannot collect specimen");
    }

    @Test
    void rejectsCancellingResultedOrder() {
        LabOrder order = orderWithStatus(LabOrderStatus.RESULTED);
        when(labOrderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> labOrderDomainService.cancel(order.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Cannot cancel");
    }

    private LabOrder orderWithStatus(LabOrderStatus status) {
        LabOrder order = LabOrder.builder()
                .patientId(UUID.randomUUID())
                .testCode("CBC")
                .testName("Complete Blood Count")
                .priority(LabPriority.ROUTINE)
                .status(status)
                .orderedAt(Instant.now())
                .orderedBy("doctor-1")
                .build();
        order.setId(UUID.randomUUID());
        return order;
    }
}
