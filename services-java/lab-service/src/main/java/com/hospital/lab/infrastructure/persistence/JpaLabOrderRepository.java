package com.hospital.lab.infrastructure.persistence;

import com.hospital.lab.domain.model.LabOrder;
import com.hospital.lab.domain.repository.LabOrderRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaLabOrderRepository extends JpaRepository<LabOrder, UUID>, LabOrderRepository {
}
