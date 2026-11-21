package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.dto.user.UserSummaryResponseDto;
import com.uit.petrescueapi.application.port.out.UserSearchPort;
import com.uit.petrescueapi.infrastructure.persistence.projection.UserSummaryProjection;
import com.uit.petrescueapi.infrastructure.persistence.repository.UserQueryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserSearchAdapter implements UserSearchPort {

    private final UserQueryJpaRepository queryRepo;

    @Override
    public Page<UserSummaryResponseDto> searchUsers(String searchName, Pageable pageable) {
        return queryRepo.findAllSummaryWithSearch(searchName, pageable).map(this::toSummaryDto);
    }

    private UserSummaryResponseDto toSummaryDto(UserSummaryProjection p) {
        return UserSummaryResponseDto.builder()
                .userId(p.getUserId())
                .userCode(p.getUserCode())
                .username(p.getUsername())
                .fullName(p.getFullName())
                .email(p.getEmail())
                .status(p.getStatus())
                .build();
    }
}
