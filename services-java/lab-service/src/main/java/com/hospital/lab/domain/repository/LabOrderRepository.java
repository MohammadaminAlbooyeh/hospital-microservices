package com.hospital.lab.domain.repository;

import com.hospital.lab.domain.model.LabOrder;
import com.hospital.lab.domain.model.LabOrderStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabOrderRepository {

    LabOrder save(LabOrder order);

    Optional<LabOrder> findById(UUID id);

    List<LabOrder> findAllByOrderByOrderedAtDesc();

    List<LabOrder> findByPatientIdOrderByOrderedAtDesc(UUID patientId);

    List<LabOrder> findByStatusOrderByOrderedAtDesc(LabOrderStatus status);

    List<LabOrder> findByPatientIdAndStatusOrderByOrderedAtDesc(UUID patientId, LabOrderStatus status);
}
