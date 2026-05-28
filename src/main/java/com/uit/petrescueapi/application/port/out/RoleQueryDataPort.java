package com.uit.petrescueapi.application.port.out;

import com.uit.petrescueapi.application.dto.role.PermissionResponseDto;
import com.uit.petrescueapi.application.dto.role.RoleResponseDto;
import com.uit.petrescueapi.application.dto.role.RoleSummaryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Output port for Role read operations (CQRS query side).
 *
 * <p>The infrastructure layer implements this to execute optimized
 * queries and map projections directly to application DTOs.</p>
 */
public interface RoleQueryDataPort {

    Page<RoleSummaryResponseDto> findAllSummaries(String search, Pageable pageable);

    RoleResponseDto findById(Integer roleId);

    List<PermissionResponseDto> getPermissions(Integer roleId);
}
