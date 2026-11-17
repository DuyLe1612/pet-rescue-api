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
    private String petName;
    private UUID organizationId;
    private String organizationName;
    /** The adopter the pet was taken back from. */
    private UUID userId;
    private String username;
    private UUID reclaimedBy;
    private String reclaimedByUsername;
    private String reason;
    private UUID proofId;
    private Integer reputationDelta;
    private LocalDateTime reclaimedAt;
}
