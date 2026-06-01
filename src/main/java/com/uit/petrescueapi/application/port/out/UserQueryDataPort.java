package com.uit.petrescueapi.application.port.out;

import com.uit.petrescueapi.application.dto.user.UserReputationResponseDto;
import com.uit.petrescueapi.application.dto.user.UserResponseDto;
import com.uit.petrescueapi.application.dto.user.UserPublicSearchDto;
import com.uit.petrescueapi.application.dto.user.UserSummaryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Output port for User read operations (CQRS query side).
 *
 * <p>The infrastructure layer implements this to execute optimized
 * JOIN queries and map projections directly to application DTOs.</p>
 */
public interface UserQueryDataPort {

    UserResponseDto findById(UUID userId);

    Page<UserSummaryResponseDto> findAllSummaries(String searchName, Pageable pageable);

    Page<UserPublicSearchDto> searchPublicUsers(String searchName, Pageable pageable);

    UserReputationResponseDto getReputation(UUID userId);
}
