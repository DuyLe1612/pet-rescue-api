package com.uit.petrescueapi.application.dto.rescue;

import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/** One row of an organization's rescue queue. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RescueQueueItemDto {

    private UUID caseId;
    private String caseCode;
    private String species;
    private RescuePriority priority;
    private String status;
    private String description;
    private String provinceName;
    private String wardName;
    private Double latitude;
    private Double longitude;
    private LocalDateTime reportedAt;
    private boolean needsHelp;
    /** Already rescued by someone who wants to hand the animal to an organization. */
    private boolean awaitingHandover;
    private long activeClaimCount;

    // ── What the AI triage found, for staff. Never replaces what the reporter wrote. ──

    @io.swagger.v3.oas.annotations.media.Schema(description = "The higher of the reporter's priority and the model's: the queue is sorted by it. The model can only raise a case.")
    private RescuePriority effectivePriority;
    @io.swagger.v3.oas.annotations.media.Schema(description = "What the model made of the report; null when the AI is off, pending or failed")
    private RescuePriority aiUrgency;
    @io.swagger.v3.oas.annotations.media.Schema(description = "Short condition summary from the model, in English")
    private String aiSummary;
    @io.swagger.v3.oas.annotations.media.Schema(description = "PENDING, ANALYZED, FAILED, or null when no triage was ever started", allowableValues = {"PENDING", "ANALYZED", "FAILED"})
    private String triageStatus;
    @io.swagger.v3.oas.annotations.media.Schema(description = "Facts the report lacks: PHOTO, DESCRIPTION, SPECIES, LOCATION. Empty when the AI is off.")
    private java.util.List<String> missingFields;
}
