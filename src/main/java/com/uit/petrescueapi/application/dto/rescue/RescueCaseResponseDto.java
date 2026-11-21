package com.uit.petrescueapi.application.dto.rescue;

import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for rescue case.
 * Excludes reportedBy, size, resolvedAt and createdAt.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Rescue case")
public class RescueCaseResponseDto {
    private UUID caseId;
    private String caseCode;
    @Schema(example = "RESCUE", allowableValues = {"RESCUE", "LOST", "FOUND"})
    private String caseType;
    @Schema(description = "FOUND only", allowableValues = {"ON_STREET", "AT_HOME", "AT_ORGANIZATION"})
    private String holdingStatus;
    private LocalDateTime lastSeenAt;
    private String distinctiveMarks;
    @Schema(description = "How the case ended, e.g. REUNITED")
    private String resolution;
    @Schema(description = "The lost or found case this one was matched with")
    private UUID matchedCaseId;
    @Schema(description = "Set when latitude and longitude are moved by up to this many metres to protect someone's home")
    private Integer locationBlurMeters;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Double blurredLatitude;
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Double blurredLongitude;
    /** The reporter's display name (full name, or "Thành viên PawHome"). Never the username. */
    private String reporterName;
    private String species;
    private String color;
    @Schema(description = "Priority level", example = "HIGH")
    private RescuePriority priority;
    private String description;
    @Schema(example = "IN_PROGRESS", allowableValues = {"REPORTED", "IN_PROGRESS", "RESCUED", "CLOSED"})
    private String status;
    private Double latitude;
    private Double longitude;
    private String locationText;
    private String wardName;
    private String provinceName;
    private LocalDateTime reportedAt;
    private List<String> imageUrls;
    @Schema(description = "Only for the reporter, people with an active claim, the handling organization and admins")
    private String contactPhone;
    private UUID organizationId;
    private String organizationName;
    private UUID petId;
    private String petName;
    private boolean needsHelp;
    private int activeClaimCount;
    private boolean viewerIsReporter;
    private boolean viewerHasActiveClaim;
    @Schema(description = "People who flagged the case as false, handled or a duplicate")
    private int flagCount;
    private boolean viewerHasFlagged;
    private UUID completionId;
    private String completionStatus;

    /** Only in the response to a report sent with shareAsPost: the post that was published. */
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private UUID sharedPostId;

    /** Only in that response: true when the case was saved but the post could not be published. */
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    private Boolean postShareFailed;
}
