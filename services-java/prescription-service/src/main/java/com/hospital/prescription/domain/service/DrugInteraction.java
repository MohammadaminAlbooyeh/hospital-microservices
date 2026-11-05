package com.hospital.prescription.domain.service;

public record DrugInteraction(
        String drugA,
        String drugB,
        InteractionSeverity severity,
        String description
) {

    public String summary() {
        return drugA + " + " + drugB + " (" + severity + "): " + description;
    }
}
