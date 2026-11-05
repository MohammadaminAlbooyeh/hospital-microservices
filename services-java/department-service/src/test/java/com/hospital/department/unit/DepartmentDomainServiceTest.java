package com.hospital.department.unit;

import com.hospital.department.domain.model.Bed;
import com.hospital.department.domain.model.BedStatus;
import com.hospital.department.domain.model.Department;
import com.hospital.department.domain.repository.BedRepository;
import com.hospital.department.domain.repository.DepartmentRepository;
import com.hospital.department.domain.repository.EquipmentRepository;
import com.hospital.department.domain.repository.RoomRepository;
import com.hospital.department.domain.service.DepartmentDomainService;
import com.hospital.shared.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentDomainServiceTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BedRepository bedRepository;

    @Mock
    private EquipmentRepository equipmentRepository;

    @InjectMocks
    private DepartmentDomainService departmentDomainService;

    @Test
    void createsADepartment() {
        when(departmentRepository.existsByName("Cardiology")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Department created = departmentDomainService.create(
                Department.builder().name("Cardiology").floor("3").build());

        assertThat(created.getName()).isEqualTo("Cardiology");
    }

    @Test
    void rejectsDuplicateDepartmentName() {
        when(departmentRepository.existsByName("Cardiology")).thenReturn(true);

        assertThatThrownBy(() -> departmentDomainService.create(
                Department.builder().name("Cardiology").build()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void computesRoomAvailabilityFromBedStatuses() {
        UUID roomId = UUID.randomUUID();
        when(bedRepository.findByRoomId(roomId)).thenReturn(List.of(
                Bed.builder().roomId(roomId).bedNumber("1").status(BedStatus.AVAILABLE).build(),
                Bed.builder().roomId(roomId).bedNumber("2").status(BedStatus.OCCUPIED).build()));

        var availability = departmentDomainService.roomAvailability(roomId);

        assertThat(availability.get("totalBeds")).isEqualTo(2);
        assertThat(availability.get("availableBeds")).isEqualTo(1L);
    }
}
