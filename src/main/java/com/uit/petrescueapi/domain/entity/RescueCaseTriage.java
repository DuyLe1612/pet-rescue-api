package com.uit.petrescueapi.domain.entity;

import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import com.uit.petrescueapi.domain.valueobject.TriageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * What the model made of a reported case. Staff read it next to the reporter's own data; it never
 * replaces that data.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RescueCaseTriage {
    private UUID caseId;
    private TriageStatus status;
    private int attempts;
    private String model;
    private Integer promptVersion;
    /** Null when the model saw no emergency. */
    private RescuePriority aiUrgency;
    private Boolean needsRescue;
    private String conditionSummary;
    private String species;
    private String district;
    private String province;
    /** Facts a rescuer would want and the report lacks, found by rules (PHOTO, DESCRIPTION, SPECIES, LOCATION). */
    private List<String> missingFields;
    private String failure;
    /** What the model returned, kept so runs can be audited and models compared. */
    private String rawOutput;
    private Long latencyMs;
    private Long inputTokens;
    private Long outputTokens;
    private LocalDateTime analyzedAt;
    private LocalDateTime updatedAt;

    public boolean isAnalyzed() {
        return status == TriageStatus.ANALYZED;
    }

    /**
     * The priority a queue should sort by: the higher of what the reporter chose and what the model
     * found. The model can only raise a priority, so a wrong answer never hides a case.
     */
    public static RescuePriority effectivePriority(RescuePriority reporter, RescueCaseTriage triage) {
        RescuePriority ai = triage != null && triage.isAnalyzed() ? triage.getAiUrgency() : null;
        if (reporter == null) {
            return ai;
        }
        if (ai == null) {
            return reporter;
        }
        // CRITICAL is declared first, so the smaller ordinal is the more urgent
        return ai.ordinal() < reporter.ordinal() ? ai : reporter;
    }
}
