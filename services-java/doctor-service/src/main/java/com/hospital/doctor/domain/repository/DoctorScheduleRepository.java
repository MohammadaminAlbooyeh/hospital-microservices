package com.hospital.doctor.domain.repository;

import com.hospital.doctor.domain.model.DoctorSchedule;

import java.util.List;
import java.util.UUID;

public interface DoctorScheduleRepository {

    DoctorSchedule save(DoctorSchedule schedule);

    List<DoctorSchedule> findByDoctorId(UUID doctorId);

    void deleteById(UUID id);
}
