package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.dto.role.RoleResponseDto;
import com.uit.petrescueapi.application.dto.role.RoleSummaryResponseDto;
import com.uit.petrescueapi.application.port.out.RoleQueryDataPort;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.infrastructure.persistence.projection.RoleDetailProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.RoleSummaryProjection;
import com.uit.petrescueapi.infrastructure.persistence.repository.RoleQueryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Query-side adapter (CQRS read path) for Role.
 *
 * <p>Executes optimized queries via {@link RoleQueryJpaRepository},
 * maps infrastructure projections to application DTOs.</p>
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleQueryAdapter implements RoleQueryDataPort {

    private final RoleQueryJpaRepository queryRepo;

    // ── List (summary) queries ──────────────────

    @Override
    public Page<RoleSummaryResponseDto> findAllSummaries(String search, Pageable pageable) {
        return queryRepo.findAllSummary(search, pageable).map(this::toSummaryDto);
    }

    // ── Detail (single role) query ──────────────

    @Override
    public RoleResponseDto findById(Integer roleId) {
        RoleDetailProjection proj = queryRepo.findDetailById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "roleId", roleId));
        return toResponseDto(proj);
    }

    // ── Projection → DTO mappers ────────────────

    private RoleSummaryResponseDto toSummaryDto(RoleSummaryProjection p) {
        return RoleSummaryResponseDto.builder()
                .roleId(p.getRoleId())
                .code(p.getCode())
                .name(p.getName())
                .build();
    }

    private RoleResponseDto toResponseDto(RoleDetailProjection p) {
        return RoleResponseDto.builder()
                .roleId(p.getRoleId())
                .code(p.getCode())
                .name(p.getName())
                .description(p.getDescription())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
