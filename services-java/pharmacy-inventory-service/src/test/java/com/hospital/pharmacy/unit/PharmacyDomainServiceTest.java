package com.hospital.pharmacy.unit;

import com.hospital.pharmacy.domain.model.DispenseRecord;
import com.hospital.pharmacy.domain.model.DispenseStatus;
import com.hospital.pharmacy.domain.model.Drug;
import com.hospital.pharmacy.domain.model.MovementType;
import com.hospital.pharmacy.domain.model.StockMovement;
import com.hospital.pharmacy.domain.repository.DispenseRecordRepository;
import com.hospital.pharmacy.domain.repository.DrugRepository;
import com.hospital.pharmacy.domain.repository.StockMovementRepository;
import com.hospital.pharmacy.domain.service.PharmacyDomainService;
import com.hospital.shared.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PharmacyDomainServiceTest {

    @Mock
    private DrugRepository drugRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private DispenseRecordRepository dispenseRecordRepository;

    @InjectMocks
    private PharmacyDomainService pharmacyDomainService;

    @Test
    void rejectsDispenseWhenStockIsInsufficient() {
        when(dispenseRecordRepository.findByIdempotencyKey("key-low")).thenReturn(Optional.empty());
        when(drugRepository.findByCode("DRG-1")).thenReturn(Optional.of(drug(2)));
        when(drugRepository.decrementStock("DRG-1", 5)).thenReturn(0);

        assertThatThrownBy(() -> pharmacyDomainService.dispense(
                UUID.randomUUID(), null, null, "DRG-1", 5, "key-low"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Insufficient stock");

        verify(dispenseRecordRepository, never()).save(any(DispenseRecord.class));
        verify(stockMovementRepository, never()).save(any(StockMovement.class));
    }

    @Test
    void returnsExistingRecordForRepeatedIdempotencyKey() {
        DispenseRecord existing = DispenseRecord.builder()
                .idempotencyKey("key-duplicate")
                .patientId(UUID.randomUUID())
                .drugCode("DRG-1")
                .quantity(2)
                .status(DispenseStatus.DISPENSED)
                .build();
        existing.setId(UUID.randomUUID());
        when(dispenseRecordRepository.findByIdempotencyKey("key-duplicate"))
                .thenReturn(Optional.of(existing));

        DispenseRecord result = pharmacyDomainService.dispense(
                UUID.randomUUID(), null, null, "DRG-1", 2, "key-duplicate");

        assertThat(result).isSameAs(existing);
        verifyNoInteractions(drugRepository, stockMovementRepository);
    }

    @Test
    void dispensesAndRecordsNegativeStockMovement() {
        when(dispenseRecordRepository.findByIdempotencyKey("key-new")).thenReturn(Optional.empty());
        when(drugRepository.findByCode("DRG-1")).thenReturn(Optional.of(drug(10)));
        when(drugRepository.decrementStock("DRG-1", 3)).thenReturn(1);
        when(dispenseRecordRepository.save(any(DispenseRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(stockMovementRepository.save(any(StockMovement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DispenseRecord result = pharmacyDomainService.dispense(
                UUID.randomUUID(), null, null, "DRG-1", 3, "key-new");

        assertThat(result.getStatus()).isEqualTo(DispenseStatus.DISPENSED);
        assertThat(result.getQuantity()).isEqualTo(3);
        ArgumentCaptor<StockMovement> movement = ArgumentCaptor.forClass(StockMovement.class);
        verify(stockMovementRepository).save(movement.capture());
        assertThat(movement.getValue().getType()).isEqualTo(MovementType.DISPENSE);
        assertThat(movement.getValue().getQuantity()).isEqualTo(-3);
        assertThat(movement.getValue().getReference()).isEqualTo("DISPENSE:key-new");
    }

    private Drug drug(int quantity) {
        return Drug.builder()
                .code("DRG-1")
                .name("Paracetamol")
                .unit("tablet")
                .unitPrice(new BigDecimal("1.50"))
                .quantityOnHand(quantity)
                .reorderLevel(5)
                .build();
    }
}
