package com.hospital.department.api;

import com.hospital.department.api.dto.BedRequest;
import com.hospital.department.api.dto.BedResponse;
import com.hospital.department.api.dto.DepartmentRequest;
import com.hospital.department.api.dto.DepartmentResponse;
import com.hospital.department.api.dto.EquipmentRequest;
import com.hospital.department.api.dto.EquipmentResponse;
import com.hospital.department.api.dto.RoomRequest;
import com.hospital.department.api.dto.RoomResponse;
import com.hospital.department.api.mapper.DepartmentMapper;
import com.hospital.department.application.CreateDepartmentUseCase;
import com.hospital.department.domain.model.BedStatus;
import com.hospital.department.domain.model.Department;
import com.hospital.department.domain.service.DepartmentDomainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DepartmentController {

    private final CreateDepartmentUseCase createDepartmentUseCase;
    private final DepartmentDomainService departmentDomainService;

    @PostMapping("/api/v1/departments")
    public ResponseEntity<DepartmentResponse> create(@Valid @RequestBody DepartmentRequest request) {
        Department department = createDepartmentUseCase.execute(request);
        return ResponseEntity.created(URI.create("/api/v1/departments/" + department.getId()))
                .body(DepartmentMapper.toResponse(department));
    }

    @GetMapping("/api/v1/departments/{id}")
    public DepartmentResponse get(@PathVariable UUID id) {
        return DepartmentMapper.toResponse(departmentDomainService.get(id));
    }

    @GetMapping("/api/v1/departments")
    public List<DepartmentResponse> list() {
        return departmentDomainService.list().stream().map(DepartmentMapper::toResponse).toList();
    }

    @PutMapping("/api/v1/departments/{id}")
    public DepartmentResponse update(@PathVariable UUID id, @Valid @RequestBody DepartmentRequest request) {
        return DepartmentMapper.toResponse(departmentDomainService.update(id, DepartmentMapper.toEntity(request)));
    }

    @DeleteMapping("/api/v1/departments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        departmentDomainService.delete(id);
    }

    @PostMapping("/api/v1/departments/{id}/rooms")
    public ResponseEntity<RoomResponse> addRoom(@PathVariable UUID id, @Valid @RequestBody RoomRequest request) {
        var room = departmentDomainService.addRoom(id, DepartmentMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(DepartmentMapper.toResponse(room));
    }

    @GetMapping("/api/v1/departments/{id}/rooms")
    public List<RoomResponse> getRooms(@PathVariable UUID id) {
        return departmentDomainService.getRooms(id).stream().map(DepartmentMapper::toResponse).toList();
    }

    @PostMapping("/api/v1/rooms/{roomId}/beds")
    public ResponseEntity<BedResponse> addBed(@PathVariable UUID roomId, @Valid @RequestBody BedRequest request) {
        var bed = departmentDomainService.addBed(roomId, DepartmentMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(DepartmentMapper.toResponse(bed));
    }

    @GetMapping("/api/v1/rooms/{roomId}/beds")
    public List<BedResponse> getBeds(@PathVariable UUID roomId) {
        return departmentDomainService.getBedsByRoom(roomId).stream().map(DepartmentMapper::toResponse).toList();
    }

    @GetMapping("/api/v1/rooms/{roomId}/availability")
    public Map<String, Object> roomAvailability(@PathVariable UUID roomId) {
        return departmentDomainService.roomAvailability(roomId);
    }

    @GetMapping("/api/v1/beds")
    public List<BedResponse> availableBeds() {
        return departmentDomainService.getAvailableBeds().stream().map(DepartmentMapper::toResponse).toList();
    }

    @PutMapping("/api/v1/beds/{bedId}/status")
    public BedResponse updateBedStatus(@PathVariable UUID bedId, @RequestParam BedStatus status) {
        return DepartmentMapper.toResponse(departmentDomainService.updateBedStatus(bedId, status));
    }

    @PostMapping("/api/v1/departments/{id}/equipment")
    public ResponseEntity<EquipmentResponse> addEquipment(@PathVariable UUID id,
                                                            @Valid @RequestBody EquipmentRequest request) {
        var equipment = departmentDomainService.addEquipment(id, DepartmentMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(DepartmentMapper.toResponse(equipment));
    }

    @GetMapping("/api/v1/departments/{id}/equipment")
    public List<EquipmentResponse> getEquipment(@PathVariable UUID id) {
        return departmentDomainService.getEquipment(id).stream().map(DepartmentMapper::toResponse).toList();
    }
}
