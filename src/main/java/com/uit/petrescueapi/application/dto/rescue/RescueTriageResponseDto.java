package com.uit.petrescueapi.application.dto.rescue;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.uit.petrescueapi.domain.entity.RescueCaseTriage;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** What the AI made of a reported case, for staff. It sits next to the report and never replaces it. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "AI triage of a rescue case")
public class RescueTriageResponseDto {

    private UUID caseId;

    @Schema(description = "NOT_STARTED when the AI is off or has not reached the case yet", allowableValues = {"NOT_STARTED", "PENDING", "ANALYZED", "FAILED"})
    private String status;

    @Schema(description = "What the reporter chose")
    private RescuePriority reporterPriority;

    @Schema(description = "What the model made of the report; null unless ANALYZED and it saw an emergency")
    private RescuePriority aiUrgency;

    @Schema(description = "The higher of the two. The model can only raise a case.")
    private RescuePriority effectivePriority;

    private Boolean needsRescue;
    @Schema(description = "Short condition summary from the model, in English")
    private String conditionSummary;
    @Schema(description = "Species from the catalog")
    private String species;
    private String district;
    private String province;
    @Schema(description = "Facts the report lacks: PHOTO, DESCRIPTION, SPECIES, LOCATION")
    private List<String> missingFields;

    private String model;
    private int attempts;
    @Schema(description = "Why the last attempt failed")
    private String failure;
    private LocalDateTime analyzedAt;

    public static RescueTriageResponseDto notStarted(UUID caseId, RescuePriority reporterPriority) {
        return RescueTriageResponseDto.builder().caseId(caseId).status("NOT_STARTED")
                .reporterPriority(reporterPriority).effectivePriority(reporterPriority).missingFields(List.of()).build();
    }

    public static RescueTriageResponseDto from(RescueCaseTriage t, RescuePriority reporterPriority) {
        return RescueTriageResponseDto.builder()
                .caseId(t.getCaseId())
                .status(t.getStatus().name())
                .reporterPriority(reporterPriority)
                .aiUrgency(t.isAnalyzed() ? t.getAiUrgency() : null)
                .effectivePriority(RescueCaseTriage.effectivePriority(reporterPriority, t))
                .needsRescue(t.isAnalyzed() ? t.getNeedsRescue() : null)
                .conditionSummary(t.isAnalyzed() ? t.getConditionSummary() : null)
                .species(t.isAnalyzed() ? t.getSpecies() : null)
                .district(t.isAnalyzed() ? t.getDistrict() : null)
                .province(t.isAnalyzed() ? t.getProvince() : null)
                .missingFields(t.isAnalyzed() && t.getMissingFields() != null ? t.getMissingFields() : List.of())
                .model(t.getModel())
                .attempts(t.getAttempts())
                .failure(t.getFailure())
                .analyzedAt(t.getAnalyzedAt())
                .build();
    }
}
