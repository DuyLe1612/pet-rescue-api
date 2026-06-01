package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.user.UserReputationResponseDto;
import com.uit.petrescueapi.application.dto.user.UserResponseDto;
import com.uit.petrescueapi.application.dto.user.UserPublicSearchDto;
import com.uit.petrescueapi.application.dto.user.UserSummaryResponseDto;
import com.uit.petrescueapi.application.port.out.UserQueryDataPort;
import com.uit.petrescueapi.application.port.query.UserQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Query (read) use-case for User operations.
 *
 * <p>Thin orchestrator: delegates directly to {@link UserQueryDataPort}
 * (implemented by the infrastructure query adapter). No domain service
 * involvement -- queries bypass the domain layer entirely (CQRS).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserQueryUseCase implements UserQueryPort {

    private final UserQueryDataPort queryDataPort;

    @Override
    public UserResponseDto findById(UUID userId) {
        log.debug("Query: find user by id {}", userId);
        return queryDataPort.findById(userId);
    }

    @Override
    public Page<UserSummaryResponseDto> findAll(String searchName, Pageable pageable) {
        log.debug("Query: find all users (paginated, searchName={})", searchName);
        return queryDataPort.findAllSummaries(searchName, pageable);
    }

    @Override
    public Page<UserPublicSearchDto> searchPublicUsers(String searchName, Pageable pageable) {
        log.debug("Query: search public users (searchName={})", searchName);
        return queryDataPort.searchPublicUsers(searchName, pageable);
    }

    @Override
    public UserReputationResponseDto getReputation(UUID userId) {
        log.debug("Query: get reputation for user {}", userId);
        return queryDataPort.getReputation(userId);
    }
}
