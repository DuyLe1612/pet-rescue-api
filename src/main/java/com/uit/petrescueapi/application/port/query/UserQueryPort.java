package com.uit.petrescueapi.application.port.query;

import com.uit.petrescueapi.application.dto.user.UserReputationResponseDto;
import com.uit.petrescueapi.application.dto.user.UserResponseDto;
import com.uit.petrescueapi.application.dto.user.UserPublicSearchDto;
import com.uit.petrescueapi.application.dto.user.UserSummaryResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserQueryPort {
    UserResponseDto findById(UUID userId);
    Page<UserSummaryResponseDto> findAll(String searchName, Pageable pageable);
    Page<UserPublicSearchDto> searchPublicUsers(String searchName, Pageable pageable);
    UserReputationResponseDto getReputation(UUID userId);
}
