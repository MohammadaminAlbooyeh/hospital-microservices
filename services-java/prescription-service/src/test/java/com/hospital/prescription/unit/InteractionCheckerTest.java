package com.hospital.prescription.unit;

import com.hospital.prescription.domain.service.DrugInteraction;
import com.hospital.prescription.domain.service.InteractionChecker;
import com.hospital.prescription.domain.service.InteractionResult;
import com.hospital.prescription.domain.service.InteractionSeverity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InteractionCheckerTest {

    private final InteractionChecker interactionChecker = new InteractionChecker();

    @Test
    void detectsSevereInteractions() {
        InteractionResult result = interactionChecker.check(
                List.of("warfarin", "aspirin", "atorvastatin", "clarithromycin"));

        assertThat(result.hasSevere()).isTrue();
        assertThat(result.severe())
                .extracting(interaction -> interaction.drugA() + "+" + interaction.drugB())
                .contains("warfarin+aspirin");
        assertThat(result.severe()).allMatch(interaction ->
                interaction.severity() == InteractionSeverity.SEVERE);
        assertThat(result.moderate())
                .extracting(DrugInteraction::description)
                .anyMatch(description -> description.toLowerCase().contains("statin"));
    }

    @Test
    void detectsModerateInteractionsAsWarnings() {
        InteractionResult result = interactionChecker.check(
                List.of("metformin", "contrast", "lisinopril", "potassium-chloride"));

        assertThat(result.hasSevere()).isFalse();
        assertThat(result.moderate()).hasSize(2);
        assertThat(result.moderate())
                .extracting(interaction -> interaction.drugA() + "+" + interaction.drugB())
                .containsExactlyInAnyOrder("contrast+metformin", "ace-inhibitor+potassium");
        assertThat(result.moderate()).allMatch(interaction ->
                interaction.severity() == InteractionSeverity.MODERATE);
    }

    @Test
    void returnsNoInteractionsForSafeCombinations() {
        InteractionResult result = interactionChecker.check(List.of("paracetamol", "amoxicillin"));

        assertThat(result.severe()).isEmpty();
        assertThat(result.moderate()).isEmpty();
    }
}
