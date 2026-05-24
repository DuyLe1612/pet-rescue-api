package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.Role;
import com.uit.petrescueapi.domain.entity.User;
import com.uit.petrescueapi.domain.exception.ResourceAlreadyExistsException;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.repository.RoleRepository;
import com.uit.petrescueapi.domain.repository.UserRepository;
import com.uit.petrescueapi.domain.repository.VisualCodeRepository;
import com.uit.petrescueapi.domain.valueobject.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Domain service for user-profile business rules.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserDomainService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final VisualCodeRepository visualCodeRepository;

    @Transactional(readOnly = true)
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    /**
     * Create a user with specified role (for admin/dev operations).
     * Password must be pre-hashed by the caller.
     */
    public User createUser(String username, String email, String hashedPassword, String roleCode) {
        if (userRepository.existsByEmail(email)) {
            throw new ResourceAlreadyExistsException("User", "email", email);
        }
        if (userRepository.existsByUsername(username)) {
            throw new ResourceAlreadyExistsException("User", "username", username);
        }

        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "code", roleCode));

        User user = User.builder()
                .id(UUID.randomUUID())
                .userCode(visualCodeRepository.nextUserCode())
                .username(username)
                .email(email)
                .passwordHash(hashedPassword)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)  // Auto-verified for admin-created accounts
                .roles(Set.of(role))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    /**
     * Create a fully custom ACTIVE user account (admin operation).
     * Password must be pre-hashed by the caller.
     */
    public User createCustomActiveUser(
            String username,
            String email,
            String hashedPassword,
            String roleCode,
            String fullName,
            String avatarUrl,
            String phone,
            String gender,
            String streetAddress,
            String wardCode,
            String wardName,
            String provinceCode,
            String provinceName
    ) {
        if (userRepository.existsByEmail(email)) {
            throw new ResourceAlreadyExistsException("User", "email", email);
        }
        if (userRepository.existsByUsername(username)) {
            throw new ResourceAlreadyExistsException("User", "username", username);
        }

        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "code", roleCode));

        User user = User.builder()
                .id(UUID.randomUUID())
                .userCode(visualCodeRepository.nextUserCode())
                .username(username)
                .email(email)
                .passwordHash(hashedPassword)
                .fullName(fullName)
                .avatarUrl(avatarUrl)
                .phone(phone)
                .gender(gender)
                .streetAddress(streetAddress)
                .wardCode(wardCode)
                .wardName(wardName)
                .provinceCode(provinceCode)
                .provinceName(provinceName)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .roles(Set.of(role))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    public User updateProfile(
            UUID id,
            String username,
            String fullName,
            String avatarUrl,
            String phone,
            String gender,
            String streetAddress,
            String wardCode,
            String wardName,
            String provinceCode,
            String provinceName
    ) {
        User user = findById(id);
        if (username != null) user.setUsername(username);
        if (fullName != null) user.setFullName(fullName);
        if (avatarUrl != null) user.setAvatarUrl(avatarUrl);
        if (phone != null) user.setPhone(phone);
        if (gender != null) user.setGender(gender);
        if (streetAddress != null) user.setStreetAddress(streetAddress);
        if (wardCode != null) user.setWardCode(wardCode);
        if (wardName != null) user.setWardName(wardName);
        if (provinceCode != null) user.setProvinceCode(provinceCode);
        if (provinceName != null) user.setProvinceName(provinceName);
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public User lockAccount(UUID id) {
        User user = findById(id);
        user.lock();
        return userRepository.save(user);
    }

    public User unlockAccount(UUID id) {
        User user = findById(id);
        user.activate();
        return userRepository.save(user);
    }

    public User updateExpoPushToken(UUID id, String token) {
        User user = findById(id);
        user.setExpoPushToken(token);
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    /**
     * Check if a user has a specific role.
     */
    @Transactional(readOnly = true)
    public boolean hasRole(UUID userId, String roleCode) {
        User user = findById(userId);
        return user.getRoles().stream()
                .anyMatch(role -> roleCode.equals(role.getCode()));
    }
}
