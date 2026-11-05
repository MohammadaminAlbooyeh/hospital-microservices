package com.hospital.department.api.mapper;

import com.hospital.department.api.dto.BedRequest;
import com.hospital.department.api.dto.BedResponse;
import com.hospital.department.api.dto.DepartmentRequest;
import com.hospital.department.api.dto.DepartmentResponse;
import com.hospital.department.api.dto.EquipmentRequest;
import com.hospital.department.api.dto.EquipmentResponse;
import com.hospital.department.api.dto.RoomRequest;
import com.hospital.department.api.dto.RoomResponse;
import com.hospital.department.domain.model.Bed;
import com.hospital.department.domain.model.BedStatus;
import com.hospital.department.domain.model.Department;
import com.hospital.department.domain.model.Equipment;
import com.hospital.department.domain.model.EquipmentStatus;
import com.hospital.department.domain.model.Room;

public final class DepartmentMapper {

    private DepartmentMapper() {
    }

    public static Department toEntity(DepartmentRequest request) {
        return Department.builder()
                .name(request.name())
                .floor(request.floor())
                .phone(request.phone())
                .build();
    }

    public static DepartmentResponse toResponse(Department department) {
        return new DepartmentResponse(department.getId(), department.getName(), department.getFloor(),
                department.getPhone(), department.getVersion(), department.getCreatedAt(),
                department.getUpdatedAt());
    }

    public static Room toEntity(RoomRequest request) {
        return Room.builder()
                .roomNumber(request.roomNumber())
                .roomType(request.roomType())
                .build();
    }

    public static RoomResponse toResponse(Room room) {
        return new RoomResponse(room.getId(), room.getDepartmentId(), room.getRoomNumber(), room.getRoomType());
    }

    public static Bed toEntity(BedRequest request) {
        return Bed.builder()
                .bedNumber(request.bedNumber())
                .status(request.status() == null ? BedStatus.AVAILABLE : request.status())
                .build();
    }

    public static BedResponse toResponse(Bed bed) {
        return new BedResponse(bed.getId(), bed.getRoomId(), bed.getBedNumber(), bed.getStatus());
    }

    public static Equipment toEntity(EquipmentRequest request) {
        return Equipment.builder()
                .name(request.name())
                .quantity(request.quantity())
                .status(request.status() == null ? EquipmentStatus.OPERATIONAL : request.status())
                .build();
    }

    public static EquipmentResponse toResponse(Equipment equipment) {
        return new EquipmentResponse(equipment.getId(), equipment.getDepartmentId(), equipment.getName(),
                equipment.getQuantity(), equipment.getStatus());
    }
}
