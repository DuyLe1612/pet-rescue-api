package com.uit.petrescueapi.domain.entity;

import com.uit.petrescueapi.domain.valueobject.RescueClaimReleaseReason;
import com.uit.petrescueapi.domain.valueobject.RescueClaimStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Someone taking responsibility for a rescue case. A claim with {@code organizationId} is held by
 * the organization and locks the case to it; a personal claim locks nothing and expires.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RescueCaseClaim {

    private UUID claimId;
    private UUID caseId;
    private UUID userId;
    private UUID organizationId;
    private RescueClaimStatus status;
    private RescueClaimReleaseReason releaseReason;
    private LocalDateTime claimedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime remindedAt;
    private LocalDateTime endedAt;
}
