package com.hospital.encounter.domain.service;

import com.hospital.encounter.domain.model.Encounter;
import com.hospital.encounter.domain.model.EncounterState;
import com.hospital.shared.exception.ApiException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class EncounterStateMachine {

    private static final Map<EncounterState, Set<EncounterState>> ALLOWED_TRANSITIONS;

    static {
        Map<EncounterState, Set<EncounterState>> transitions = new EnumMap<>(EncounterState.class);
        transitions.put(EncounterState.CREATED,
                EnumSet.of(EncounterState.CHECKED_IN, EncounterState.CANCELLED));
        transitions.put(EncounterState.CHECKED_IN,
                EnumSet.of(EncounterState.IN_CONSULTATION, EncounterState.CANCELLED));
        transitions.put(EncounterState.IN_CONSULTATION,
                EnumSet.of(EncounterState.LAB_PENDING, EncounterState.PHARMACY_PENDING,
                        EncounterState.COMPLETED, EncounterState.CANCELLED));
        transitions.put(EncounterState.LAB_PENDING,
                EnumSet.of(EncounterState.PHARMACY_PENDING, EncounterState.COMPLETED,
                        EncounterState.CANCELLED));
        transitions.put(EncounterState.PHARMACY_PENDING,
                EnumSet.of(EncounterState.COMPLETED, EncounterState.CANCELLED));
        transitions.put(EncounterState.COMPLETED, EnumSet.noneOf(EncounterState.class));
        transitions.put(EncounterState.CANCELLED, EnumSet.noneOf(EncounterState.class));

        Map<EncounterState, Set<EncounterState>> immutable = new EnumMap<>(EncounterState.class);
        transitions.forEach((state, targets) -> immutable.put(state, Collections.unmodifiableSet(targets)));
        ALLOWED_TRANSITIONS = Collections.unmodifiableMap(immutable);
    }

    public Encounter transition(Encounter encounter, EncounterState target) {
        if (encounter == null) {
            throw ApiException.badRequest("Encounter is required");
        }
        if (target == null) {
            throw ApiException.badRequest("Target encounter state is required");
        }
        EncounterState current = encounter.getState();
        if (current == null) {
            throw ApiException.conflict("Encounter has no current state");
        }
        Set<EncounterState> allowed = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw ApiException.conflict("Illegal encounter state transition: " + current + " -> " + target);
        }
        encounter.setState(target);
        Instant now = Instant.now();
        switch (target) {
            case CHECKED_IN -> encounter.setCheckedInAt(now);
            case IN_CONSULTATION -> encounter.setStartedAt(now);
            case COMPLETED -> encounter.setCompletedAt(now);
            case CANCELLED -> encounter.setCancelledAt(now);
            default -> {
            }
        }
        return encounter;
    }

    public boolean canTransition(EncounterState from, EncounterState to) {
        return ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }
}
