package com.hospital.department.infrastructure.persistence;

import com.hospital.department.domain.model.Bed;
import com.hospital.department.domain.model.BedStatus;
import com.hospital.department.domain.repository.BedRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaBedRepository extends JpaRepository<Bed, UUID>, BedRepository {

    @Override
    List<Bed> findByRoomId(UUID roomId);

    @Override
    List<Bed> findByStatus(BedStatus status);
}
