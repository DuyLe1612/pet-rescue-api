package com.uit.petrescueapi.application.dto.admin;

import java.util.List;
import java.util.Map;

/**
 * Numbers for the evaluation chapter of the report. Every rate is null while its denominator is
 * zero. They count whatever is in the database, demo data included.
 */
public final class EvaluationMetricsDto {

    private EvaluationMetricsDto() {
    }

    public record Matching(List<Level> byLevel, Warnings warnings, TraitCoverage traitCoverage, String note) {
    }

    /** Approved counts APPROVED and COMPLETED; decided adds REJECTED. Outcomes come from follow-up. */
    public record Level(String level, long submitted, long decided, long approved, Double approvalRate,
                        Map<String, Long> outcomes) {
    }

    public record Warnings(long scored, long withWarnings, Double warningRate,
                           long decidedWithWarnings, long approvedWithWarnings, Double approvedWarningRate) {
    }

    public record TraitCoverage(long availablePets, long withTraits, Double rate) {
    }

    public record Rescue(List<ClaimTime> timeToFirstClaim, Triage triage, LostAndFound lostAndFound,
                         long openFlaggedCases, String note) {
    }

    /** Rescue cases only. triaged means the AI labelled the case. */
    public record ClaimTime(String priority, boolean triaged, long cases, long claimed, Double medianMinutes) {
    }

    /** The AI's urgency against the case's current priority, which staff may have corrected. */
    public record Triage(long analyzed, long agree, long aiHigher, long aiLower, long aiNoEmergency, long criticalMissed) {
    }

    public record LostAndFound(long openLost, long openFound, long suggestions, long pending, long confirmed,
                               long rejected, Double confirmRate, long reunitedLostCases, Double medianHoursToReunion) {
    }
}
