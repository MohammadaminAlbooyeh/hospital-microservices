package com.hospital.appointment.api;

import com.hospital.appointment.api.dto.AppointmentResponse;
import com.hospital.appointment.api.dto.BookAppointmentRequest;
import com.hospital.appointment.api.dto.CancelAppointmentRequest;
import com.hospital.appointment.api.dto.RescheduleAppointmentRequest;
import com.hospital.appointment.api.mapper.AppointmentMapper;
import com.hospital.appointment.application.BookAppointmentUseCase;
import com.hospital.appointment.application.CancelAppointmentUseCase;
import com.hospital.appointment.application.RescheduleAppointmentUseCase;
import com.hospital.appointment.domain.model.Appointment;
import com.hospital.appointment.domain.service.AppointmentDomainService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final BookAppointmentUseCase bookAppointmentUseCase;
    private final CancelAppointmentUseCase cancelAppointmentUseCase;
    private final RescheduleAppointmentUseCase rescheduleAppointmentUseCase;
    private final AppointmentDomainService appointmentDomainService;

    @PostMapping
    public ResponseEntity<AppointmentResponse> book(@Valid @RequestBody BookAppointmentRequest request) {
        Appointment appointment = bookAppointmentUseCase.execute(request);
        return ResponseEntity.created(URI.create("/api/v1/appointments/" + appointment.getId()))
                .body(AppointmentMapper.toResponse(appointment));
    }

    @GetMapping("/{id}")
    public AppointmentResponse get(@PathVariable UUID id) {
        return AppointmentMapper.toResponse(appointmentDomainService.get(id));
    }

    @GetMapping
    public List<AppointmentResponse> search(@RequestParam(required = false) UUID patientId,
                                             @RequestParam(required = false) UUID doctorId,
                                             @RequestParam(required = false) LocalDate date) {
        List<Appointment> result;
        if (doctorId != null && date != null) {
            result = appointmentDomainService.findByDoctorAndDate(doctorId, date);
        } else if (patientId != null) {
            result = appointmentDomainService.findByPatient(patientId);
        } else if (doctorId != null) {
            result = appointmentDomainService.findByDoctor(doctorId);
        } else {
            result = List.of();
        }
        return result.stream().map(AppointmentMapper::toResponse).toList();
    }

    @PutMapping("/{id}/reschedule")
    public AppointmentResponse reschedule(@PathVariable UUID id,
                                           @Valid @RequestBody RescheduleAppointmentRequest request) {
        return AppointmentMapper.toResponse(
                rescheduleAppointmentUseCase.execute(id, request.slotStart(), request.slotEnd()));
    }

    @PutMapping("/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable UUID id,
                                       @RequestBody(required = false) CancelAppointmentRequest request) {
        String reason = request == null ? null : request.reason();
        return AppointmentMapper.toResponse(cancelAppointmentUseCase.execute(id, reason));
    }
}
