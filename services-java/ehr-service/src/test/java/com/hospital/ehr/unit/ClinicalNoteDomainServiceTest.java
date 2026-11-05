package com.hospital.ehr.unit;

import com.hospital.ehr.domain.model.ClinicalNote;
import com.hospital.ehr.domain.repository.ClinicalNoteRepository;
import com.hospital.ehr.domain.service.ClinicalNoteDomainService;
import com.hospital.shared.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicalNoteDomainServiceTest {

    @Mock
    private ClinicalNoteRepository clinicalNoteRepository;

    @InjectMocks
    private ClinicalNoteDomainService clinicalNoteDomainService;

    @Test
    void assignsTheNextVersionAndSupersedesThePreviousNote() {
        UUID patientId = UUID.randomUUID();
        ClinicalNote previous = ClinicalNote.builder()
                .patientId(patientId)
                .clinicalVersion(2)
                .content("Previous note")
                .build();
        previous.setId(UUID.randomUUID());

        when(clinicalNoteRepository.findTopByPatientIdOrderByClinicalVersionDesc(patientId))
                .thenReturn(Optional.of(previous));
        when(clinicalNoteRepository.saveAndFlush(any(ClinicalNote.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClinicalNote created = clinicalNoteDomainService.createVersion(
                patientId, UUID.randomUUID(), UUID.randomUUID(), "Updated note", List.of("I10", "E11.9"));

        assertThat(created.getClinicalVersion()).isEqualTo(3);
        assertThat(created.getSupersedesId()).isEqualTo(previous.getId());
        assertThat(created.diagnosisList()).containsExactly("I10", "E11.9");
    }

    @Test
    void startsAtVersionOneWhenThePatientHasNoNotes() {
        UUID patientId = UUID.randomUUID();

        when(clinicalNoteRepository.findTopByPatientIdOrderByClinicalVersionDesc(patientId))
                .thenReturn(Optional.empty());
        when(clinicalNoteRepository.saveAndFlush(any(ClinicalNote.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ClinicalNote created = clinicalNoteDomainService.createVersion(
                patientId, null, null, "First note", List.of());

        assertThat(created.getClinicalVersion()).isEqualTo(1);
        assertThat(created.getSupersedesId()).isNull();
    }

    @Test
    void returnsTheLatestVersionAsTheCurrentNote() {
        UUID patientId = UUID.randomUUID();
        ClinicalNote latest = ClinicalNote.builder()
                .patientId(patientId)
                .clinicalVersion(4)
                .content("Latest")
                .build();
        when(clinicalNoteRepository.findTopByPatientIdOrderByClinicalVersionDesc(patientId))
                .thenReturn(Optional.of(latest));

        assertThat(clinicalNoteDomainService.current(patientId)).isSameAs(latest);
    }

    @Test
    void rejectsEmptyContent() {
        assertThatThrownBy(() -> clinicalNoteDomainService.createVersion(
                UUID.randomUUID(), null, null, "  ", List.of()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("must not be empty");
    }
}
