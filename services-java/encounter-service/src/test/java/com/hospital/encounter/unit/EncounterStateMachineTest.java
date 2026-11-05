package com.hospital.encounter.unit;

import com.hospital.encounter.domain.model.Encounter;
import com.hospital.encounter.domain.model.EncounterState;
import com.hospital.encounter.domain.service.EncounterStateMachine;
import com.hospital.shared.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncounterStateMachineTest {

    private final EncounterStateMachine stateMachine = new EncounterStateMachine();

    @Test
    void allowsTheFullConsultationPath() {
        Encounter encounter = encounter(EncounterState.CREATED);

        stateMachine.transition(encounter, EncounterState.CHECKED_IN);
        assertThat(encounter.getState()).isEqualTo(EncounterState.CHECKED_IN);
        assertThat(encounter.getCheckedInAt()).isNotNull();

        stateMachine.transition(encounter, EncounterState.IN_CONSULTATION);
        assertThat(encounter.getStartedAt()).isNotNull();

        stateMachine.transition(encounter, EncounterState.LAB_PENDING);
        stateMachine.transition(encounter, EncounterState.PHARMACY_PENDING);
        stateMachine.transition(encounter, EncounterState.COMPLETED);

        assertThat(encounter.getState()).isEqualTo(EncounterState.COMPLETED);
        assertThat(encounter.getCompletedAt()).isNotNull();
    }

    @Test
    void allowsCancellationFromActiveStates() {
        Encounter encounter = encounter(EncounterState.IN_CONSULTATION);

        stateMachine.transition(encounter, EncounterState.CANCELLED);

        assertThat(encounter.getState()).isEqualTo(EncounterState.CANCELLED);
        assertThat(encounter.getCancelledAt()).isNotNull();
        assertThatThrownBy(() -> stateMachine.transition(encounter, EncounterState.CHECKED_IN))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Illegal encounter state transition");
    }

    @Test
    void rejectsIllegalTransitions() {
        Encounter encounter = encounter(EncounterState.CREATED);

        assertThatThrownBy(() -> stateMachine.transition(encounter, EncounterState.COMPLETED))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("CREATED -> COMPLETED");

        Encounter completed = encounter(EncounterState.COMPLETED);
        assertThatThrownBy(() -> stateMachine.transition(completed, EncounterState.CANCELLED))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("COMPLETED -> CANCELLED");
    }

    @Test
    void exposesAllowedTransitionLookup() {
        assertThat(stateMachine.canTransition(EncounterState.CHECKED_IN, EncounterState.IN_CONSULTATION)).isTrue();
        assertThat(stateMachine.canTransition(EncounterState.COMPLETED, EncounterState.CHECKED_IN)).isFalse();
    }

    private Encounter encounter(EncounterState state) {
        return Encounter.builder()
                .patientId(UUID.randomUUID())
                .state(state)
                .build();
    }
}
