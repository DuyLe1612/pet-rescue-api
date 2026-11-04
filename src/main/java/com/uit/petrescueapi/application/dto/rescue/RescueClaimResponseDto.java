package com.uit.petrescueapi.application.dto.rescue;

import com.uit.petrescueapi.domain.entity.RescueCaseClaim;
import com.uit.petrescueapi.domain.valueobject.RescueClaimReleaseReason;
import com.uit.petrescueapi.domain.valueobject.RescueClaimStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RescueClaimResponseDto {

    private UUID claimId;
    private UUID caseId;
    private String caseCode;
    private UUID userId;
    private String username;
    private UUID organizationId;
    private RescueClaimStatus status;
    private RescueClaimReleaseReason releaseReason;
    private LocalDateTime claimedAt;
    private LocalDateTime expiresAt;

    public static RescueClaimResponseDto from(RescueCaseClaim claim) {
        return RescueClaimResponseDto.builder()
                .claimId(claim.getClaimId())
                .caseId(claim.getCaseId())
                .userId(claim.getUserId())
                .organizationId(claim.getOrganizationId())
                .status(claim.getStatus())
                .releaseReason(claim.getReleaseReason())
                .claimedAt(claim.getClaimedAt())
                .expiresAt(claim.getExpiresAt())
                .build();
    }
}
