package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.RescueCaseClaim;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Domain repository contract for rescue case claims. */
public interface RescueCaseClaimRepository {

    RescueCaseClaim save(RescueCaseClaim claim);

    Optional<RescueCaseClaim> findById(UUID claimId);

    List<RescueCaseClaim> findActiveByCaseId(UUID caseId);

    long countActiveByCaseId(UUID caseId);

    Optional<RescueCaseClaim> findActiveByCaseAndUser(UUID caseId, UUID userId);

    Optional<RescueCaseClaim> findActiveByCaseAndOrganization(UUID caseId, UUID organizationId);

    List<RescueCaseClaim> findActiveByUser(UUID userId);

    /** Personal claims only; organization claims never expire. */
    long countActivePersonalByUser(UUID userId);

    List<RescueCaseClaim> findExpiredBefore(LocalDateTime time, int limit);

    List<RescueCaseClaim> findToRemindBefore(LocalDateTime time, int limit);
}
