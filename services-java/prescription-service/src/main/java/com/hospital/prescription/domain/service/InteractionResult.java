package com.hospital.prescription.domain.service;

import java.util.List;
import java.util.stream.Collectors;

public record InteractionResult(
        List<DrugInteraction> severe,
        List<DrugInteraction> moderate
) {

    public boolean hasSevere() {
        return severe != null && !severe.isEmpty();
    }

    public String severeSummary() {
        if (severe == null || severe.isEmpty()) {
            return "";
        }
        return severe.stream().map(DrugInteraction::summary).collect(Collectors.joining("; "));
    }
}
