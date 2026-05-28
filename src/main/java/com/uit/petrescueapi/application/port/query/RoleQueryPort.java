package com.uit.petrescueapi.application.port.query;

import com.uit.petrescueapi.application.dto.role.PermissionResponseDto;
import com.uit.petrescueapi.application.dto.role.RoleResponseDto;
import com.uit.petrescueapi.application.dto.role.RoleSummaryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RoleQueryPort {
    Page<RoleSummaryResponseDto> findAll(String search, Pageable pageable);
    RoleResponseDto findById(Integer roleId);
    List<PermissionResponseDto> getPermissions(Integer roleId);
}
