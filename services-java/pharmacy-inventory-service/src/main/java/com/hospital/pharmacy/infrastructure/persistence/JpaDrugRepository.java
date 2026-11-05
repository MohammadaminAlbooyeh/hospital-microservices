package com.hospital.pharmacy.infrastructure.persistence;

import com.hospital.pharmacy.domain.model.Drug;
import com.hospital.pharmacy.domain.repository.DrugRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaDrugRepository extends JpaRepository<Drug, UUID>, DrugRepository {
}
