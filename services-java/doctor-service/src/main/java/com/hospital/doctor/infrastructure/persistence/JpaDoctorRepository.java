package com.hospital.doctor.infrastructure.persistence;

import com.hospital.doctor.domain.model.Doctor;
import com.hospital.doctor.domain.repository.DoctorRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaDoctorRepository extends JpaRepository<Doctor, UUID>, DoctorRepository {

    @Override
    List<Doctor> findBySpeciality(String speciality);
}
