package com.hospital.doctor.infrastructure.persistence;

import com.hospital.doctor.domain.model.DoctorSchedule;
import com.hospital.doctor.domain.repository.DoctorScheduleRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaDoctorScheduleRepository extends JpaRepository<DoctorSchedule, UUID>, DoctorScheduleRepository {

    @Override
    List<DoctorSchedule> findByDoctorId(UUID doctorId);
}
