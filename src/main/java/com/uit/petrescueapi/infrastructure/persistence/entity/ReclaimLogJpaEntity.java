package com.uit.petrescueapi.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reclaim_logs", indexes = {
        @Index(name = "idx_reclaim_application", columnList = "application_id"),
        @Index(name = "idx_reclaim_org", columnList = "organization_id"),
        @Index(name = "idx_reclaim_user", columnList = "user_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ReclaimLogJpaEntity extends BaseJpaEntity {
    @Id
    @Column(name = "reclaim_id", updatable = false, nullable = false)
    private UUID reclaimId;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "pet_id", nullable = false)
    private UUID petId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "reclaimed_by", nullable = false)
    private UUID reclaimedBy;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "proof_id", nullable = false)
    private UUID proofId;

    @Column(name = "reputation_delta", nullable = false)
    private Integer reputationDelta;

    @Column(name = "reclaimed_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private LocalDateTime reclaimedAt;
}
