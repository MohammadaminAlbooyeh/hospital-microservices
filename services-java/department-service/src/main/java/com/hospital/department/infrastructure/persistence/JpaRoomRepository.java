package com.hospital.department.infrastructure.persistence;

import com.hospital.department.domain.model.Room;
import com.hospital.department.domain.repository.RoomRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaRoomRepository extends JpaRepository<Room, UUID>, RoomRepository {

    @Override
    List<Room> findByDepartmentId(UUID departmentId);
}
