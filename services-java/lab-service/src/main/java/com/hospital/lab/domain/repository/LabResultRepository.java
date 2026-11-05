package com.hospital.lab.domain.repository;

import com.hospital.lab.domain.model.LabResult;

import java.util.Optional;
import java.util.UUID;

public interface LabResultRepository {

    LabResult save(LabResult result);

    Optional<LabResult> findByOrderId(UUID orderId);

    boolean existsByOrderId(UUID orderId);
}
