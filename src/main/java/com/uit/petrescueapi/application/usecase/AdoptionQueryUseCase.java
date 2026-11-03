package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.adoption.AdoptionResponseDto;
import com.uit.petrescueapi.application.dto.adoption.AdoptionSummaryResponseDto;
import com.uit.petrescueapi.application.port.out.AdoptionQueryDataPort;
import com.uit.petrescueapi.application.port.query.AdoptionQueryPort;
import com.uit.petrescueapi.domain.entity.AdoptionApplication;
import com.uit.petrescueapi.domain.service.AccessControlDomainService;
import com.uit.petrescueapi.domain.service.AdoptionDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Query (read) use-case for Adoption operations.
 *
 * <p>Thin orchestrator: delegates directly to {@link AdoptionQueryDataPort}
 * (implemented by the infrastructure query adapter). No domain service
 * involvement -- queries bypass the domain layer entirely (CQRS).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdoptionQueryUseCase implements AdoptionQueryPort {

    private final AdoptionQueryDataPort queryDataPort;
    private final AdoptionDomainService adoptionDomainService;
    private final AccessControlDomainService accessControl;

    @Override
    public AdoptionResponseDto findById(UUID applicationId, UUID requesterId) {
        log.debug("Query: find adoption application by id {}", applicationId);
        AdoptionApplication application = adoptionDomainService.findById(applicationId);
        accessControl.requireAdoptionViewer(application.getApplicantId(), application.getOrganizationId(), requesterId);
        return queryDataPort.findById(applicationId);
    }

    @Override
    public Page<AdoptionSummaryResponseDto> findAll(List<String> statuses, String search, Pageable pageable, UUID requesterId) {
        accessControl.requireAdmin(requesterId);
        log.debug("Query: find all adoption applications (statuses={}, search={}, paginated)", statuses, search);
        return queryDataPort.findAllSummaries(statuses, search, pageable);
    }

    @Override
    public Page<AdoptionSummaryResponseDto> findByApplicantId(UUID applicantId, List<String> statuses, String search, Pageable pageable, UUID requesterId) {
        accessControl.requireSelfOrAdmin(applicantId, requesterId);
        log.debug("Query: find adoption applications by applicant id {} (statuses={}, search={}, paginated)", applicantId, statuses, search);
        return queryDataPort.findByApplicantIdSummaries(applicantId, statuses, search, pageable);
    }

    @Override
    public void requireReclaimLogAccess(UUID organizationId, UUID requesterId) {
        if (organizationId == null) {
            accessControl.requireAdmin(requesterId);
        } else {
            accessControl.requireOrgMemberOrAdmin(organizationId, requesterId);
        }
    }

    @Override
    public Page<AdoptionSummaryResponseDto> findByOrganizationId(UUID organizationId, List<String> statuses, String search, Pageable pageable, UUID requesterId) {
        accessControl.requireOrgMemberOrAdmin(organizationId, requesterId);
        log.debug("Query: find adoption applications by organization id {} (statuses={}, search={}, paginated)", organizationId, statuses, search);
        return queryDataPort.findByOrganizationIdSummaries(organizationId, statuses, search, pageable);
    }
}
