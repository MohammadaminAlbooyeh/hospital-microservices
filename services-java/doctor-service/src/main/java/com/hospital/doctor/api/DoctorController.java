package com.hospital.doctor.api;

import com.hospital.doctor.api.dto.DoctorRequest;
import com.hospital.doctor.api.dto.DoctorResponse;
import com.hospital.doctor.api.dto.LeaveRequest;
import com.hospital.doctor.api.dto.LeaveResponse;
import com.hospital.doctor.api.dto.ScheduleRequest;
import com.hospital.doctor.api.dto.ScheduleResponse;
import com.hospital.doctor.api.mapper.DoctorMapper;
import com.hospital.doctor.application.RegisterDoctorUseCase;
import com.hospital.doctor.domain.model.Doctor;
import com.hospital.doctor.domain.service.DoctorDomainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final RegisterDoctorUseCase registerDoctorUseCase;
    private final DoctorDomainService doctorDomainService;

    @PostMapping
    public ResponseEntity<DoctorResponse> register(@Valid @RequestBody DoctorRequest request) {
        Doctor doctor = registerDoctorUseCase.execute(request);
        return ResponseEntity.created(URI.create("/api/v1/doctors/" + doctor.getId()))
                .body(DoctorMapper.toResponse(doctor));
    }

    @GetMapping("/{id}")
    public DoctorResponse get(@PathVariable UUID id) {
        return DoctorMapper.toResponse(doctorDomainService.get(id));
    }

    @GetMapping
    public List<DoctorResponse> list(@RequestParam(required = false) String speciality) {
        List<Doctor> doctors = speciality == null
                ? doctorDomainService.list()
                : doctorDomainService.findBySpeciality(speciality);
        return doctors.stream().map(DoctorMapper::toResponse).toList();
    }

    @PutMapping("/{id}")
    public DoctorResponse update(@PathVariable UUID id, @Valid @RequestBody DoctorRequest request) {
        return DoctorMapper.toResponse(doctorDomainService.update(id, DoctorMapper.toEntity(request)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        doctorDomainService.delete(id);
    }

    @PostMapping("/{id}/schedule")
    public ResponseEntity<ScheduleResponse> addSchedule(@PathVariable UUID id,
                                                          @Valid @RequestBody ScheduleRequest request) {
        var schedule = doctorDomainService.addSchedule(id, DoctorMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(DoctorMapper.toResponse(schedule));
    }

    @GetMapping("/{id}/schedule")
    public List<ScheduleResponse> getSchedule(@PathVariable UUID id) {
        return doctorDomainService.getSchedule(id).stream().map(DoctorMapper::toResponse).toList();
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<LeaveResponse> addLeave(@PathVariable UUID id, @Valid @RequestBody LeaveRequest request) {
        var leave = doctorDomainService.addLeave(id, DoctorMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(DoctorMapper.toResponse(leave));
    }

    @GetMapping("/{id}/leave")
    public List<LeaveResponse> getLeave(@PathVariable UUID id) {
        return doctorDomainService.getLeave(id).stream().map(DoctorMapper::toResponse).toList();
    }

    @GetMapping("/{id}/availability")
    public Map<String, Object> availability(@PathVariable UUID id, @RequestParam LocalDate date) {
        return doctorDomainService.availability(id, date);
    }
}
