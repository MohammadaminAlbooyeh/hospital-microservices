package com.hospital.doctor.api.mapper;

import com.hospital.doctor.api.dto.DoctorRequest;
import com.hospital.doctor.api.dto.DoctorResponse;
import com.hospital.doctor.api.dto.LeaveRequest;
import com.hospital.doctor.api.dto.LeaveResponse;
import com.hospital.doctor.api.dto.ScheduleRequest;
import com.hospital.doctor.api.dto.ScheduleResponse;
import com.hospital.doctor.domain.model.Doctor;
import com.hospital.doctor.domain.model.DoctorLeave;
import com.hospital.doctor.domain.model.DoctorSchedule;

public final class DoctorMapper {

    private DoctorMapper() {
    }

    public static Doctor toEntity(DoctorRequest request) {
        return Doctor.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .speciality(request.speciality())
                .email(request.email())
                .phone(request.phone())
                .licenseNumber(request.licenseNumber())
                .departmentId(request.departmentId())
                .dailyCapacity(request.dailyCapacity())
                .build();
    }

    public static DoctorResponse toResponse(Doctor doctor) {
        return new DoctorResponse(
                doctor.getId(), doctor.getFirstName(), doctor.getLastName(), doctor.getSpeciality(),
                doctor.getEmail(), doctor.getPhone(), doctor.getLicenseNumber(), doctor.getDepartmentId(),
                doctor.getDailyCapacity(), doctor.isActive(), doctor.getVersion(),
                doctor.getCreatedAt(), doctor.getUpdatedAt());
    }

    public static DoctorSchedule toEntity(ScheduleRequest request) {
        return DoctorSchedule.builder()
                .dayOfWeek(request.dayOfWeek())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .maxAppointments(request.maxAppointments() == null ? 10 : request.maxAppointments())
                .build();
    }

    public static ScheduleResponse toResponse(DoctorSchedule schedule) {
        return new ScheduleResponse(schedule.getId(), schedule.getDoctorId(), schedule.getDayOfWeek(),
                schedule.getStartTime(), schedule.getEndTime(), schedule.getMaxAppointments());
    }

    public static DoctorLeave toEntity(LeaveRequest request) {
        return DoctorLeave.builder()
                .startDate(request.startDate())
                .endDate(request.endDate())
                .reason(request.reason())
                .build();
    }

    public static LeaveResponse toResponse(DoctorLeave leave) {
        return new LeaveResponse(leave.getId(), leave.getDoctorId(), leave.getStartDate(), leave.getEndDate(),
                leave.getReason());
    }
}
