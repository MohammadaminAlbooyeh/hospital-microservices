package com.hospital.pharmacy.infrastructure.persistence;

import com.hospital.pharmacy.domain.model.DispenseRecord;
import com.hospital.pharmacy.domain.repository.DispenseRecordRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaDispenseRecordRepository extends JpaRepository<DispenseRecord, UUID>, DispenseRecordRepository {
}
