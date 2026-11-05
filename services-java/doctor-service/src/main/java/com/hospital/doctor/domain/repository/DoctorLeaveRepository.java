package com.hospital.doctor.domain.repository;

import com.hospital.doctor.domain.model.DoctorLeave;

import java.util.List;
import java.util.UUID;

public interface DoctorLeaveRepository {

    DoctorLeave save(DoctorLeave leave);

    List<DoctorLeave> findByDoctorId(UUID doctorId);

    void deleteById(UUID id);
}
