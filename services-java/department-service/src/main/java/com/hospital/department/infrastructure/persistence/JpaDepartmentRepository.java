package com.hospital.department.infrastructure.persistence;

import com.hospital.department.domain.model.Department;
import com.hospital.department.domain.repository.DepartmentRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaDepartmentRepository extends JpaRepository<Department, UUID>, DepartmentRepository {
}
