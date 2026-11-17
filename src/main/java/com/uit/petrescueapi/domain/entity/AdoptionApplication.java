package com.uit.petrescueapi.domain.entity;

import com.uit.petrescueapi.domain.valueobject.AdoptionApplicationStatus;
import com.uit.petrescueapi.domain.valueobject.HousingCondition;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AdoptionApplication — represents an adoption request from a user.
 * Extends BaseEntity for audit fields.
 * Pure domain entity: no JPA annotations.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AdoptionApplication extends BaseEntity {

    private UUID applicationId;
    private String adoptionCode;
    private UUID petId;
    private UUID applicantId;
    private UUID organizationId;
    private AdoptionApplicationStatus status;
    private String note;
    private String experience;
    private HousingCondition housingCondition;
    private String liveCondition;
    private UUID signatureMediaId;
    private LocalDateTime decidedAt;
    private UUID decidedBy;
    private String rejectReason;
    private LocalDateTime readyAt;

    // Match result frozen at submission; never recomputed, so history survives a change of weights.
    private String matchLevel;
    private java.math.BigDecimal matchScore;
    private String matchCertainty;
    private Integer matchWeightsVersion;
    /** Comma-separated warning codes, null when none fired. */
    private String matchWarnings;
    private boolean warningsAcknowledged;
    /** Reviewer sort key: the score, -1 without a match, -2 when a warning fired. */
    @lombok.Builder.Default
    private java.math.BigDecimal matchRank = java.math.BigDecimal.valueOf(-1);
    /** JSON text of both profiles and the full result. */
    private String matchSnapshot;
    private String decisionReasonCode;

    public boolean hasMatchWarnings() {
        return matchWarnings != null && !matchWarnings.isBlank();
    }
}
