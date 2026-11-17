package com.uit.petrescueapi.domain.entity;

import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * RescueCase — represents a reported animal rescue case.
 * Extends BaseEntity for audit fields.
 * Pure domain entity: no JPA annotations.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RescueCase extends BaseEntity {

    private UUID caseId;
    private String caseCode;
    private UUID reportedBy;
    private UUID organizationId;
    private UUID petId;
    private String species;
    private String color;
    private String size;
    private RescuePriority priority;
    private String description;
    private Double latitude;
    private Double longitude;
    private String locationText;
    private String provinceCode;
    private String provinceName;
    private String wardCode;
    private String wardName;
    private RescueCaseStatus status;
    private LocalDateTime reportedAt;
    private LocalDateTime resolvedAt;
    private List<String> imagePublicIds;
    private String contactPhone;
    private Long version;
    private String resolutionNote;
    private boolean needsHelp;
    /** Null is read as RESCUE, so cases built before case types keep behaving as rescues. */
    private com.uit.petrescueapi.domain.valueobject.RescueCaseType caseType;
    private com.uit.petrescueapi.domain.valueobject.HoldingStatus holdingStatus;
    private LocalDateTime lastSeenAt;
    private String distinctiveMarks;
    private String resolution;
    private UUID matchedCaseId;
    private String petStatusBeforeLost;

    public com.uit.petrescueapi.domain.valueobject.RescueCaseType typeOrRescue() {
        return caseType == null ? com.uit.petrescueapi.domain.valueobject.RescueCaseType.RESCUE : caseType;
    }
}
