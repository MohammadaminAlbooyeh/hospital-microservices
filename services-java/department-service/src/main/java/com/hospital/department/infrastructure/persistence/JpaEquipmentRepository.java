package com.hospital.department.infrastructure.persistence;

import com.hospital.department.domain.model.Equipment;
import com.hospital.department.domain.repository.EquipmentRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaEquipmentRepository extends JpaRepository<Equipment, UUID>, EquipmentRepository {

    @Override
    List<Equipment> findByDepartmentId(UUID departmentId);
}
