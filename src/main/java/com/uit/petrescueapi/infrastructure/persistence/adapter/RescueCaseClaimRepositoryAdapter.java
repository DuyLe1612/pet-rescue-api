package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.RescueCaseClaim;
import com.uit.petrescueapi.domain.repository.RescueCaseClaimRepository;
import com.uit.petrescueapi.domain.valueobject.RescueClaimReleaseReason;
import com.uit.petrescueapi.domain.valueobject.RescueClaimStatus;
import com.uit.petrescueapi.infrastructure.persistence.entity.RescueCaseClaimJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.repository.RescueCaseClaimJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RescueCaseClaimRepositoryAdapter implements RescueCaseClaimRepository {

    private static final String ACTIVE = RescueClaimStatus.ACTIVE.name();

    private final RescueCaseClaimJpaRepository jpa;

    @Override
    public RescueCaseClaim save(RescueCaseClaim claim) {
        return toDomain(jpa.save(toEntity(claim)));
    }

    @Override
    public Optional<RescueCaseClaim> findById(UUID claimId) {
        return jpa.findById(claimId).map(RescueCaseClaimRepositoryAdapter::toDomain);
    }

    @Override
    public List<RescueCaseClaim> findActiveByCaseId(UUID caseId) {
        return jpa.findByCaseIdAndStatusOrderByClaimedAtAsc(caseId, ACTIVE).stream()
                .map(RescueCaseClaimRepositoryAdapter::toDomain).toList();
    }

    @Override
    public long countActiveByCaseId(UUID caseId) {
        return jpa.countByCaseIdAndStatus(caseId, ACTIVE);
    }

    @Override
    public Optional<RescueCaseClaim> findActiveByCaseAndUser(UUID caseId, UUID userId) {
        return jpa.findFirstByCaseIdAndUserIdAndStatus(caseId, userId, ACTIVE)
                .map(RescueCaseClaimRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<RescueCaseClaim> findActiveByCaseAndOrganization(UUID caseId, UUID organizationId) {
        return jpa.findFirstByCaseIdAndOrganizationIdAndStatus(caseId, organizationId, ACTIVE)
                .map(RescueCaseClaimRepositoryAdapter::toDomain);
    }

    @Override
    public List<RescueCaseClaim> findActiveByUser(UUID userId) {
        return jpa.findByUserIdAndStatusOrderByClaimedAtDesc(userId, ACTIVE).stream()
                .map(RescueCaseClaimRepositoryAdapter::toDomain).toList();
    }

    @Override
    public long countActivePersonalByUser(UUID userId) {
        return jpa.countByUserIdAndStatusAndOrganizationIdIsNull(userId, ACTIVE);
    }

    @Override
    public List<RescueCaseClaim> findExpiredBefore(LocalDateTime time, int limit) {
        return jpa.findExpiredBefore(time, PageRequest.of(0, limit)).stream()
                .map(RescueCaseClaimRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<RescueCaseClaim> findToRemindBefore(LocalDateTime time, int limit) {
        return jpa.findToRemindBefore(time, PageRequest.of(0, limit)).stream()
                .map(RescueCaseClaimRepositoryAdapter::toDomain).toList();
    }

    private static RescueCaseClaim toDomain(RescueCaseClaimJpaEntity e) {
        return RescueCaseClaim.builder()
                .claimId(e.getClaimId())
                .caseId(e.getCaseId())
                .userId(e.getUserId())
                .organizationId(e.getOrganizationId())
                .status(RescueClaimStatus.valueOf(e.getStatus()))
                .releaseReason(e.getReleaseReason() == null ? null : RescueClaimReleaseReason.valueOf(e.getReleaseReason()))
                .claimedAt(e.getClaimedAt())
                .expiresAt(e.getExpiresAt())
                .remindedAt(e.getRemindedAt())
                .endedAt(e.getEndedAt())
                .build();
    }

    private static RescueCaseClaimJpaEntity toEntity(RescueCaseClaim c) {
        return RescueCaseClaimJpaEntity.builder()
                .claimId(c.getClaimId())
                .caseId(c.getCaseId())
                .userId(c.getUserId())
                .organizationId(c.getOrganizationId())
                .status(c.getStatus().name())
                .releaseReason(c.getReleaseReason() == null ? null : c.getReleaseReason().name())
                .claimedAt(c.getClaimedAt())
                .expiresAt(c.getExpiresAt())
                .remindedAt(c.getRemindedAt())
                .endedAt(c.getEndedAt())
                .build();
    }
}
