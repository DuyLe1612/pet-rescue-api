package com.uit.petrescueapi.domain.entity;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = false)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReclaimLog extends BaseEntity {
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
