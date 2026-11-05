package com.hospital.lab.domain.repository;

import com.hospital.lab.domain.model.Specimen;

import java.util.Optional;
import java.util.UUID;

public interface SpecimenRepository {

    Specimen save(Specimen specimen);

    Optional<Specimen> findByOrderId(UUID orderId);

    boolean existsByBarcode(String barcode);
}
