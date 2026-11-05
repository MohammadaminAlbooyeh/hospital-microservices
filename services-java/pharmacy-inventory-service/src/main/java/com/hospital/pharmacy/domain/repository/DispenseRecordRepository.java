package com.hospital.pharmacy.domain.repository;

import com.hospital.pharmacy.domain.model.DispenseRecord;
import com.hospital.pharmacy.domain.model.DispenseStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DispenseRecordRepository {

    DispenseRecord save(DispenseRecord record);

    Optional<DispenseRecord> findById(UUID id);

    Optional<DispenseRecord> findByIdempotencyKey(String idempotencyKey);

    List<DispenseRecord> findByIdempotencyKeyInAndStatus(Collection<String> keys, DispenseStatus status);
}
