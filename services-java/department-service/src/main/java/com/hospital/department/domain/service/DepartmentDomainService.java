package com.hospital.department.domain.service;

import com.hospital.department.domain.model.Bed;
import com.hospital.department.domain.model.BedStatus;
import com.hospital.department.domain.model.Department;
import com.hospital.department.domain.model.Equipment;
import com.hospital.department.domain.model.Room;
import com.hospital.department.domain.repository.BedRepository;
import com.hospital.department.domain.repository.DepartmentRepository;
import com.hospital.department.domain.repository.EquipmentRepository;
import com.hospital.department.domain.repository.RoomRepository;
import com.hospital.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DepartmentDomainService {

    private final DepartmentRepository departmentRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final EquipmentRepository equipmentRepository;

    @Transactional
    public Department create(Department department) {
        if (departmentRepository.existsByName(department.getName())) {
            throw ApiException.conflict("A department named " + department.getName() + " already exists");
        }
        return departmentRepository.save(department);
    }

    @Transactional
    public Department update(UUID id, Department patch) {
        Department existing = get(id);
        existing.setName(patch.getName());
        existing.setFloor(patch.getFloor());
        existing.setPhone(patch.getPhone());
        return departmentRepository.save(existing);
    }

    @Transactional(readOnly = true)
    public Department get(UUID id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Department " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<Department> list() {
        return departmentRepository.findAll();
    }

    @Transactional
    public void delete(UUID id) {
        departmentRepository.delete(get(id));
    }

    @Transactional
    public Room addRoom(UUID departmentId, Room room) {
        get(departmentId);
        room.setDepartmentId(departmentId);
        return roomRepository.save(room);
    }

    @Transactional(readOnly = true)
    public List<Room> getRooms(UUID departmentId) {
        return roomRepository.findByDepartmentId(departmentId);
    }

    @Transactional
    public Bed addBed(UUID roomId, Bed bed) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> ApiException.notFound("Room " + roomId + " not found"));
        bed.setRoomId(room.getId());
        if (bed.getStatus() == null) {
            bed.setStatus(BedStatus.AVAILABLE);
        }
        return bedRepository.save(bed);
    }

    @Transactional
    public Bed updateBedStatus(UUID bedId, BedStatus status) {
        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> ApiException.notFound("Bed " + bedId + " not found"));
        bed.setStatus(status);
        return bedRepository.save(bed);
    }

    @Transactional(readOnly = true)
    public List<Bed> getBedsByRoom(UUID roomId) {
        return bedRepository.findByRoomId(roomId);
    }

    @Transactional(readOnly = true)
    public List<Bed> getAvailableBeds() {
        return bedRepository.findByStatus(BedStatus.AVAILABLE);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> roomAvailability(UUID roomId) {
        List<Bed> beds = bedRepository.findByRoomId(roomId);
        long available = beds.stream().filter(bed -> bed.getStatus() == BedStatus.AVAILABLE).count();
        return Map.of(
                "roomId", roomId,
                "totalBeds", beds.size(),
                "availableBeds", available,
                "occupiedBeds", beds.size() - available);
    }

    @Transactional
    public Equipment addEquipment(UUID departmentId, Equipment equipment) {
        get(departmentId);
        equipment.setDepartmentId(departmentId);
        return equipmentRepository.save(equipment);
    }

    @Transactional(readOnly = true)
    public List<Equipment> getEquipment(UUID departmentId) {
        return equipmentRepository.findByDepartmentId(departmentId);
    }
}
