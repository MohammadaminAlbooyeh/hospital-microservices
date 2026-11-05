package com.hospital.department.domain.repository;

import com.hospital.department.domain.model.Equipment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EquipmentRepository {

    Equipment save(Equipment equipment);

    Optional<Equipment> findById(UUID id);

    List<Equipment> findByDepartmentId(UUID departmentId);
}
