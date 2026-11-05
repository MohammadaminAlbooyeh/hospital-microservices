package com.hospital.doctor.infrastructure.persistence;

import com.hospital.doctor.domain.model.DoctorLeave;
import com.hospital.doctor.domain.repository.DoctorLeaveRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaDoctorLeaveRepository extends JpaRepository<DoctorLeave, UUID>, DoctorLeaveRepository {

    @Override
    List<DoctorLeave> findByDoctorId(UUID doctorId);
}
