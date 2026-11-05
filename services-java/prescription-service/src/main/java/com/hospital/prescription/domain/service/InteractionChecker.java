package com.hospital.prescription.domain.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class InteractionChecker {

    private static final Map<String, DrugInteraction> CATALOG = Map.ofEntries(
            Map.entry(key("warfarin", "aspirin"), new DrugInteraction(
                    "warfarin", "aspirin", InteractionSeverity.SEVERE,
                    "Additive anticoagulant and antiplatelet effect markedly increases bleeding risk")),
            Map.entry(key("warfarin", "ibuprofen"), new DrugInteraction(
                    "warfarin", "ibuprofen", InteractionSeverity.SEVERE,
                    "NSAID-induced platelet inhibition and gastric injury increase bleeding risk")),
            Map.entry(key("warfarin", "clarithromycin"), new DrugInteraction(
                    "warfarin", "clarithromycin", InteractionSeverity.MODERATE,
                    "CYP inhibition can raise warfarin exposure and INR")),
            Map.entry(key("warfarin", "ssri"), new DrugInteraction(
                    "warfarin", "ssri", InteractionSeverity.MODERATE,
                    "SSRIs may potentiate anticoagulation and increase bleeding risk")),
            Map.entry(key("metformin", "contrast"), new DrugInteraction(
                    "metformin", "contrast", InteractionSeverity.MODERATE,
                    "Contrast-induced nephropathy can precipitate metformin accumulation and lactic acidosis")),
            Map.entry(key("ssri", "tramadol"), new DrugInteraction(
                    "ssri", "tramadol", InteractionSeverity.SEVERE,
                    "Serotonergic combination risks serotonin syndrome and lowers the seizure threshold")),
            Map.entry(key("statin", "clarithromycin"), new DrugInteraction(
                    "statin", "clarithromycin", InteractionSeverity.MODERATE,
                    "Macrolide CYP3A4 inhibition increases statin exposure and myopathy risk")),
            Map.entry(key("ace-inhibitor", "potassium"), new DrugInteraction(
                    "ace-inhibitor", "potassium", InteractionSeverity.MODERATE,
                    "ACE inhibition plus potassium supplementation can cause hyperkalaemia")),
            Map.entry(key("digoxin", "amiodarone"), new DrugInteraction(
                    "digoxin", "amiodarone", InteractionSeverity.SEVERE,
                    "Amiodarone reduces digoxin clearance and can cause toxicity")),
            Map.entry(key("methotrexate", "ibuprofen"), new DrugInteraction(
                    "methotrexate", "ibuprofen", InteractionSeverity.SEVERE,
                    "NSAIDs reduce methotrexate clearance and increase marrow toxicity")),
            Map.entry(key("clopidogrel", "omeprazole"), new DrugInteraction(
                    "clopidogrel", "omeprazole", InteractionSeverity.MODERATE,
                    "CYP2C19 inhibition may reduce clopidogrel activation")),
            Map.entry(key("lithium", "ibuprofen"), new DrugInteraction(
                    "lithium", "ibuprofen", InteractionSeverity.MODERATE,
                    "NSAIDs reduce lithium clearance and can cause toxicity")),
            Map.entry(key("spironolactone", "potassium"), new DrugInteraction(
                    "spironolactone", "potassium", InteractionSeverity.MODERATE,
                    "Potassium-sparing diuretic plus supplementation risks hyperkalaemia")),
            Map.entry(key("aspirin", "ibuprofen"), new DrugInteraction(
                    "aspirin", "ibuprofen", InteractionSeverity.MODERATE,
                    "Ibuprofen may interfere with low-dose aspirin cardioprotection"))
    );

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("acetylsalicylic-acid", "aspirin"),
            Map.entry("asa", "aspirin"),
            Map.entry("warfarin-sodium", "warfarin"),
            Map.entry("sertraline", "ssri"),
            Map.entry("fluoxetine", "ssri"),
            Map.entry("citalopram", "ssri"),
            Map.entry("escitalopram", "ssri"),
            Map.entry("paroxetine", "ssri"),
            Map.entry("fluvoxamine", "ssri"),
            Map.entry("venlafaxine", "ssri"),
            Map.entry("duloxetine", "ssri"),
            Map.entry("atorvastatin", "statin"),
            Map.entry("simvastatin", "statin"),
            Map.entry("rosuvastatin", "statin"),
            Map.entry("pravastatin", "statin"),
            Map.entry("lovastatin", "statin"),
            Map.entry("fluvastatin", "statin"),
            Map.entry("lisinopril", "ace-inhibitor"),
            Map.entry("enalapril", "ace-inhibitor"),
            Map.entry("ramipril", "ace-inhibitor"),
            Map.entry("captopril", "ace-inhibitor"),
            Map.entry("perindopril", "ace-inhibitor"),
            Map.entry("potassium-chloride", "potassium"),
            Map.entry("kcl", "potassium"),
            Map.entry("iodinated-contrast", "contrast"),
            Map.entry("contrast-media", "contrast"),
            Map.entry("contrast-dye", "contrast")
    );

    public InteractionResult check(Collection<String> drugCodes) {
        if (drugCodes == null || drugCodes.isEmpty()) {
            return new InteractionResult(List.of(), List.of());
        }
        List<String> canonical = drugCodes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(this::canonicalize)
                .distinct()
                .toList();
        Set<DrugInteraction> found = new LinkedHashSet<>();
        for (int i = 0; i < canonical.size(); i++) {
            for (int j = i + 1; j < canonical.size(); j++) {
                DrugInteraction interaction = CATALOG.get(key(canonical.get(i), canonical.get(j)));
                if (interaction != null) {
                    found.add(interaction);
                }
            }
        }
        List<DrugInteraction> severe = new ArrayList<>();
        List<DrugInteraction> moderate = new ArrayList<>();
        for (DrugInteraction interaction : found) {
            if (interaction.severity() == InteractionSeverity.SEVERE) {
                severe.add(interaction);
            } else {
                moderate.add(interaction);
            }
        }
        return new InteractionResult(List.copyOf(severe), List.copyOf(moderate));
    }

    public boolean interacts(String first, String second) {
        return CATALOG.containsKey(key(canonicalize(first), canonicalize(second)));
    }

    private String canonicalize(String drugCode) {
        String normalized = drugCode.trim().toLowerCase(Locale.ROOT)
                .replace('_', '-')
                .replace(' ', '-');
        return ALIASES.getOrDefault(normalized, normalized);
    }

    static String key(String first, String second) {
        return first.compareTo(second) <= 0 ? first + "|" + second : second + "|" + first;
    }
}
