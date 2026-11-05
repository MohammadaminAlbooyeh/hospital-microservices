package com.hospital.prescription.application;

import com.hospital.prescription.api.dto.IssuePrescriptionRequest;
import com.hospital.prescription.domain.model.Prescription;
import com.hospital.prescription.domain.model.PrescriptionItem;
import com.hospital.prescription.domain.model.PrescriptionStatus;
import com.hospital.prescription.domain.repository.PrescriptionRepository;
import com.hospital.prescription.domain.service.InteractionChecker;
import com.hospital.prescription.domain.service.InteractionResult;
import com.hospital.prescription.infrastructure.client.MlInteractionClient;
import com.hospital.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IssuePrescriptionUseCase {

    private final PrescriptionRepository prescriptionRepository;
    private final InteractionChecker interactionChecker;
    private final MlInteractionClient mlInteractionClient;

    @Transactional
    public IssuePrescriptionResult execute(IssuePrescriptionRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw ApiException.badRequest("A prescription must contain at least one item");
        }
        List<String> drugCodes = request.items().stream()
                .map(IssuePrescriptionRequest.ItemRequest::drugCode)
                .toList();
        InteractionResult interactions = interactionChecker.check(drugCodes);
        if (interactions.hasSevere()) {
            throw ApiException.conflict("Severe drug interactions detected: " + interactions.severeSummary());
        }

        mlInteractionClient.check(drugCodes);

        Prescription prescription = Prescription.builder()
                .patientId(request.patientId())
                .encounterId(request.encounterId())
                .doctorId(request.doctorId())
                .status(PrescriptionStatus.ISSUED)
                .notes(request.notes())
                .issuedAt(Instant.now())
                .build();

        for (IssuePrescriptionRequest.ItemRequest itemRequest : request.items()) {
            prescription.addItem(PrescriptionItem.builder()
                    .drugCode(itemRequest.drugCode())
                    .drugName(itemRequest.drugName())
                    .dose(itemRequest.dose())
                    .frequency(itemRequest.frequency())
                    .durationDays(itemRequest.durationDays())
                    .instructions(itemRequest.instructions())
                    .build());
        }

        Prescription saved = prescriptionRepository.save(prescription);
        return new IssuePrescriptionResult(saved, interactions.moderate());
    }
}
