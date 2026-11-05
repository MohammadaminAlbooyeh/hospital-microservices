package com.hospital.pharmacy.domain.service;

import com.hospital.pharmacy.domain.model.DispenseRecord;
import com.hospital.pharmacy.domain.model.DispenseStatus;
import com.hospital.pharmacy.domain.model.Drug;
import com.hospital.pharmacy.domain.model.MovementType;
import com.hospital.pharmacy.domain.model.StockMovement;
import com.hospital.pharmacy.domain.repository.DispenseRecordRepository;
import com.hospital.pharmacy.domain.repository.DrugRepository;
import com.hospital.pharmacy.domain.repository.StockMovementRepository;
import com.hospital.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PharmacyDomainService {

    private final DrugRepository drugRepository;
    private final StockMovementRepository stockMovementRepository;
    private final DispenseRecordRepository dispenseRecordRepository;

    @Transactional
    public Drug receive(Drug incoming, int quantity) {
        if (quantity < 0) {
            throw ApiException.badRequest("Received quantity must not be negative");
        }
        Drug drug = drugRepository.findByCode(incoming.getCode()).orElse(null);
        if (drug == null) {
            incoming.setQuantityOnHand(quantity);
            Drug saved = drugRepository.save(incoming);
            if (quantity > 0) {
                recordMovement(saved.getCode(), MovementType.RECEIVE, quantity, null);
            }
            return saved;
        }
        drug.setName(incoming.getName());
        drug.setForm(incoming.getForm());
        drug.setUnit(incoming.getUnit());
        drug.setUnitPrice(incoming.getUnitPrice());
        drug.setReorderLevel(incoming.getReorderLevel());
        drug.setExpiryDate(incoming.getExpiryDate());
        drug.setQuantityOnHand(drug.getQuantityOnHand() + quantity);
        Drug saved = drugRepository.save(drug);
        if (quantity > 0) {
            recordMovement(saved.getCode(), MovementType.RECEIVE, quantity, null);
        }
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Drug> listDrugs() {
        return drugRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Drug getDrug(String code) {
        return drugRepository.findByCode(code)
                .orElseThrow(() -> ApiException.notFound("Drug " + code + " not found"));
    }

    @Transactional(readOnly = true)
    public List<Drug> lowStock() {
        return drugRepository.findLowStock();
    }

    @Transactional(readOnly = true)
    public List<Drug> expiringWithin(int days) {
        if (days < 0) {
            throw ApiException.badRequest("days must be zero or greater");
        }
        return drugRepository.findByExpiryDateLessThanEqualOrderByExpiryDateAsc(LocalDate.now().plusDays(days));
    }

    @Transactional
    public DispenseRecord dispense(UUID patientId, UUID encounterId, UUID prescriptionId,
                                   String drugCode, int quantity, String idempotencyKey) {
        if (quantity <= 0) {
            throw ApiException.badRequest("Dispense quantity must be positive");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw ApiException.badRequest("Idempotency-Key header is required");
        }
        if (idempotencyKey.length() > 255) {
            throw ApiException.badRequest("Idempotency-Key must be at most 255 characters");
        }
        return dispenseRecordRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> createDispense(patientId, encounterId, prescriptionId,
                        drugCode, quantity, idempotencyKey));
    }

    private DispenseRecord createDispense(UUID patientId, UUID encounterId, UUID prescriptionId,
                                          String drugCode, int quantity, String idempotencyKey) {
        drugRepository.findByCode(drugCode)
                .orElseThrow(() -> ApiException.notFound("Drug " + drugCode + " not found"));

        int updated = drugRepository.decrementStock(drugCode, quantity);
        if (updated == 0) {
            throw ApiException.conflict("Insufficient stock for drug " + drugCode);
        }

        DispenseRecord record = DispenseRecord.builder()
                .idempotencyKey(idempotencyKey)
                .patientId(patientId)
                .encounterId(encounterId)
                .prescriptionId(prescriptionId)
                .drugCode(drugCode)
                .quantity(quantity)
                .status(DispenseStatus.DISPENSED)
                .build();
        DispenseRecord saved = dispenseRecordRepository.save(record);
        recordMovement(drugCode, MovementType.DISPENSE, -quantity, dispenseReference(idempotencyKey));
        return saved;
    }

    @Transactional(readOnly = true)
    public DispenseRecord getDispense(UUID id) {
        return dispenseRecordRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Dispense record " + id + " not found"));
    }

    @Transactional
    public int finalizeDispenses(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        List<DispenseRecord> records =
                dispenseRecordRepository.findByIdempotencyKeyInAndStatus(keys, DispenseStatus.DISPENSED);
        int finalized = 0;
        Instant now = Instant.now();
        for (DispenseRecord record : records) {
            if (record.getFinalizedAt() == null) {
                record.setFinalizedAt(now);
                dispenseRecordRepository.save(record);
                finalized++;
            }
        }
        return finalized;
    }

    @Transactional
    public int reverseDispenses(Collection<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0;
        }
        List<DispenseRecord> records =
                dispenseRecordRepository.findByIdempotencyKeyInAndStatus(keys, DispenseStatus.DISPENSED);
        int reversed = 0;
        for (DispenseRecord record : records) {
            String reversalReference = reversalReference(record.getIdempotencyKey());
            if (stockMovementRepository.existsByReference(reversalReference)) {
                markReversed(record);
                continue;
            }
            if (drugRepository.findByCode(record.getDrugCode()).isEmpty()) {
                log.warn("Cannot reverse dispense {}: drug {} no longer exists",
                        record.getId(), record.getDrugCode());
                continue;
            }
            int updated = drugRepository.incrementStock(record.getDrugCode(), record.getQuantity());
            if (updated == 0) {
                log.warn("Cannot reverse dispense {}: unable to restore stock for drug {}",
                        record.getId(), record.getDrugCode());
                continue;
            }
            recordMovement(record.getDrugCode(), MovementType.REFUND, record.getQuantity(), reversalReference);
            markReversed(record);
            reversed++;
        }
        return reversed;
    }

    private void markReversed(DispenseRecord record) {
        record.setStatus(DispenseStatus.REVERSED);
        record.setFinalizedAt(null);
        dispenseRecordRepository.save(record);
    }

    private void recordMovement(String drugCode, MovementType type, int quantity, String reference) {
        StockMovement movement = StockMovement.builder()
                .drugCode(drugCode)
                .type(type)
                .quantity(quantity)
                .reference(reference)
                .build();
        stockMovementRepository.save(movement);
    }

    private String dispenseReference(String idempotencyKey) {
        return "DISPENSE:" + idempotencyKey;
    }

    private String reversalReference(String idempotencyKey) {
        return "REVERSAL:" + idempotencyKey;
    }
}
