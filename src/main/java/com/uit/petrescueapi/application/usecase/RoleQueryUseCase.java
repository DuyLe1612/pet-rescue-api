package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.role.PermissionResponseDto;
import com.uit.petrescueapi.application.dto.role.RoleResponseDto;
import com.uit.petrescueapi.application.dto.role.RoleSummaryResponseDto;
import com.uit.petrescueapi.application.port.out.RoleQueryDataPort;
import com.uit.petrescueapi.application.port.query.RoleQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Query (read) use-case for Role operations.
 *
 * <p>Thin orchestrator: delegates directly to {@link RoleQueryDataPort}
 * (implemented by the infrastructure query adapter). No domain service
 * involvement -- queries bypass the domain layer entirely (CQRS).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoleQueryUseCase implements RoleQueryPort {

    private final RoleQueryDataPort queryDataPort;

    @Override
    public Page<RoleSummaryResponseDto> findAll(String search, Pageable pageable) {
        log.debug("Query: find all roles (paginated, search={})", search);
        return queryDataPort.findAllSummaries(search, pageable);
    }

    @Override
    public RoleResponseDto findById(Integer roleId) {
        log.debug("Query: find role by id {}", roleId);
        return queryDataPort.findById(roleId);
    }

    @Override
    public List<PermissionResponseDto> getPermissions(Integer roleId) {
        log.debug("Query: get permissions for role {}", roleId);
        return queryDataPort.getPermissions(roleId);
    }
}
