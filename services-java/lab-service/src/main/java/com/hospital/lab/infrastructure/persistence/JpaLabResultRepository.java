package com.hospital.lab.infrastructure.persistence;

import com.hospital.lab.domain.model.LabResult;
import com.hospital.lab.domain.repository.LabResultRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaLabResultRepository extends JpaRepository<LabResult, UUID>, LabResultRepository {
}
