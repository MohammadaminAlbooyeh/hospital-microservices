package com.hospital.ehr.application;

import com.hospital.ehr.api.dto.CreateClinicalNoteRequest;
import com.hospital.ehr.domain.model.ClinicalNote;
import com.hospital.ehr.domain.service.ClinicalNoteDomainService;
import com.hospital.shared.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreateClinicalNoteUseCase {

    private static final int MAX_ATTEMPTS = 3;

    private final ClinicalNoteDomainService clinicalNoteDomainService;
    private final TransactionTemplate transactionTemplate;

    public ClinicalNote execute(CreateClinicalNoteRequest request) {
        if (request.content() == null || request.content().isBlank()) {
            throw ApiException.badRequest("Clinical note content must not be empty");
        }
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                ClinicalNote note = transactionTemplate.execute(status ->
                        clinicalNoteDomainService.createVersion(
                                request.patientId(),
                                request.encounterId(),
                                request.authorId(),
                                request.content(),
                                request.diagnoses()));
                if (note == null) {
                    throw ApiException.conflict("Unable to create clinical note version");
                }
                return note;
            } catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException ex) {
                if (attempt == MAX_ATTEMPTS) {
                    throw ApiException.conflict("Clinical note version conflict for patient "
                            + request.patientId() + "; retry the request");
                }
                log.warn("Clinical note version conflict for patient {} (attempt {}), retrying",
                        request.patientId(), attempt);
            }
        }
        throw ApiException.conflict("Clinical note version conflict for patient " + request.patientId());
    }
}
