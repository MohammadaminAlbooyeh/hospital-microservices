package com.hospital.lab.infrastructure.persistence;

import com.hospital.lab.domain.model.Specimen;
import com.hospital.lab.domain.repository.SpecimenRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaSpecimenRepository extends JpaRepository<Specimen, UUID>, SpecimenRepository {
}
