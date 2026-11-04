package com.uit.petrescueapi.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "rescue_case_claims")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RescueCaseClaimJpaEntity {

    @Id
    @Column(name = "claim_id", updatable = false, nullable = false)
    private UUID claimId;

    @Column(name = "case_id", nullable = false, updatable = false)
    private UUID caseId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "release_reason", length = 30)
    private String releaseReason;

    @Column(name = "claimed_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
    private LocalDateTime claimedAt;

    @Column(name = "expires_at", columnDefinition = "TIMESTAMPTZ")
    private LocalDateTime expiresAt;

    @Column(name = "reminded_at", columnDefinition = "TIMESTAMPTZ")
    private LocalDateTime remindedAt;

    @Column(name = "ended_at", columnDefinition = "TIMESTAMPTZ")
    private LocalDateTime endedAt;
}
