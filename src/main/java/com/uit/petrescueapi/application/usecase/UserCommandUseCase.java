package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.user.UpdateUserProfileRequestDto;
import com.uit.petrescueapi.application.port.command.UserCommandPort;
import com.uit.petrescueapi.domain.entity.User;
import com.uit.petrescueapi.domain.repository.UserRepository;
import com.uit.petrescueapi.domain.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Command (write) use-case for User operations.
 * Translates parameters into domain calls and delegates business rules
 * to {@link UserDomainService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserCommandUseCase implements UserCommandPort {

    private final UserDomainService domainService;
    private final UserRepository userRepository;

    @Override
    public User updateProfile(UUID userId, UpdateUserProfileRequestDto request) {
        log.debug("Command: update profile for user {}", userId);
        return domainService.updateProfile(
                userId,
                request.getUsername(),
                request.getFullName(),
                request.getAvatarUrl(),
                request.getPhone(),
                request.getGender(),
                request.getStreetAddress(),
                request.getWardCode(),
                request.getWardName(),
                request.getProvinceCode(),
                request.getProvinceName()
        );
    }

    @Override
    public void updatePushToken(UUID userId, String expoPushToken) {
        domainService.updateExpoPushToken(userId, expoPushToken);
    }

    @Override
    public void deactivate(UUID userId) {
        log.debug("Command: deactivate user {}", userId);
        User user = domainService.findById(userId);
        user.deactivate();
        userRepository.save(user);
    }

    @Override
    public User lockAccount(UUID userId) {
        log.debug("Command: lock user {}", userId);
        return domainService.lockAccount(userId);
    }

    @Override
    public User unlockAccount(UUID userId) {
        log.debug("Command: unlock user {}", userId);
        return domainService.unlockAccount(userId);
    }
}
