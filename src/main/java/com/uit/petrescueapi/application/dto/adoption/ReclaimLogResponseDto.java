package com.uit.petrescueapi.application.dto.adoption;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReclaimLogResponseDto {
    private UUID reclaimId;
    private UUID applicationId;
    private UUID petId;
    private UUID organizationId;
    private UUID userId;
    private UUID reclaimedBy;
    private String reason;
    private UUID proofId;
    private Integer reputationDelta;
    private LocalDateTime reclaimedAt;
}
