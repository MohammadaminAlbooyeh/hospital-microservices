package com.hospital.department.domain.repository;

import com.hospital.department.domain.model.Room;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoomRepository {

    Room save(Room room);

    Optional<Room> findById(UUID id);

    List<Room> findByDepartmentId(UUID departmentId);
}
