package com.hospital.department.domain.repository;

import com.hospital.department.domain.model.Bed;
import com.hospital.department.domain.model.BedStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BedRepository {

    Bed save(Bed bed);

    Optional<Bed> findById(UUID id);

    List<Bed> findByRoomId(UUID roomId);

    List<Bed> findByStatus(BedStatus status);
}
