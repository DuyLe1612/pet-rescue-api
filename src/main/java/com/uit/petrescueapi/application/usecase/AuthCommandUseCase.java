package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.auth.AuthTokenResponseDto;
import com.uit.petrescueapi.application.dto.auth.LoginRequestDto;
import com.uit.petrescueapi.application.dto.auth.RefreshTokenRequestDto;
import com.uit.petrescueapi.application.dto.auth.RegisterRequestDto;
import com.uit.petrescueapi.application.dto.user.UserResponseDto;
import com.uit.petrescueapi.application.port.command.AuthCommandPort;
import com.uit.petrescueapi.domain.entity.EmailVerificationToken;
import com.uit.petrescueapi.domain.entity.PasswordResetToken;
import com.uit.petrescueapi.domain.entity.RefreshToken;
import com.uit.petrescueapi.domain.entity.User;
import com.uit.petrescueapi.domain.exception.AccountBannedException;
import com.uit.petrescueapi.domain.exception.AccountLockedException;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.exception.UnauthorizedException;
import com.uit.petrescueapi.domain.service.AuthDomainService;
import com.uit.petrescueapi.domain.valueobject.UserStatus;
import com.uit.petrescueapi.domain.entity.Organization;
import com.uit.petrescueapi.domain.repository.OrganizationRepository;
import com.uit.petrescueapi.infrastructure.email.EmailService;
import com.uit.petrescueapi.infrastructure.redis.LoginAttemptService;
import com.uit.petrescueapi.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Application-layer use case that orchestrates authentication workflows.
 *
 * <p>Combines domain logic ({@link AuthDomainService}) with infrastructure
 * concerns (password hashing, JWT creation, email sending) without leaking
 * those details into the domain layer.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthCommandUseCase implements AuthCommandPort {

    private final AuthDomainService authDomainService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final LoginAttemptService loginAttemptService;
    private final com.uit.petrescueapi.domain.repository.OrganizationMemberRepository organizationMemberRepository;
    private final OrganizationRepository organizationRepository;

    @Value("${app.security.refresh-token.expiry-days:7}")
    private long refreshTokenExpiryDays;

    @Value("${app.email.verification-token-expiry-minutes:1440}")
    private long verificationTokenExpiryMinutes;

    @Value("${app.email.password-reset-token-expiry-minutes:60}")
    private long passwordResetTokenExpiryMinutes;

    // ── Register ────────────────────────────────

    @Override
    public AuthTokenResponseDto register(RegisterRequestDto cmd) {
        String hashedPassword = passwordEncoder.encode(cmd.getPassword());

        User user = authDomainService.registerUser(
                cmd.getUsername(),
                cmd.getEmail(),
                hashedPassword,
                cmd.getFullName(),
                cmd.getPhone(),
                cmd.getGender(),
                cmd.getStreetAddress(),
                cmd.getWardCode(),
                cmd.getWardName(),
                cmd.getProvinceCode(),
                cmd.getProvinceName());

        // Create and send verification token
        EmailVerificationToken verificationToken =
                authDomainService.createVerificationToken(user.getId(), verificationTokenExpiryMinutes);
        emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), verificationToken.getToken());

        // Generate tokens so the user is "logged in" immediately
        return buildTokenResponse(user);
    }

    // ── Login ───────────────────────────────────

    @Override
    public AuthTokenResponseDto login(LoginRequestDto cmd) {
        String identifier = cmd.getEmailOrUsername();

        // Check if account is rate-limited
        if (loginAttemptService.isRateLimited(identifier)) {
            if (loginAttemptService.isLocked(identifier)) {
                long remainingSeconds = loginAttemptService.getRemainingLockoutSeconds(identifier);
                log.warn("Login blocked - too many failed attempts for: {}", maskIdentifier(identifier));
                throw new AccountLockedException(remainingSeconds);
            }
        }

        // Check if account is locked in Redis
        if (loginAttemptService.isLocked(identifier)) {
            long remainingSeconds = loginAttemptService.getRemainingLockoutSeconds(identifier);
            log.warn("Login blocked - account is locked: {}", maskIdentifier(identifier));
            throw new AccountLockedException(remainingSeconds);
        }

        // Find user by email or username
        User user = authDomainService.findByEmailOrUsername(identifier);

        // Check account status BEFORE password verification for security
        if (user.getStatus() == UserStatus.BANNED) {
            log.warn("Login attempt on banned account: {}", maskIdentifier(identifier));
            throw new AccountBannedException();
        }

        if (user.getStatus() == UserStatus.LOCKED) {
            log.warn("Login attempt on locked account: {}", maskIdentifier(identifier));
            throw new AccountLockedException("Your account is temporarily locked. Please try again later.");
        }

        // Verify password
        if (!passwordEncoder.matches(cmd.getPassword(), user.getPasswordHash())) {
            int attempts = loginAttemptService.recordFailedAttempt(identifier);
            int remainingAttempts = 5 - attempts;
            if (remainingAttempts > 0) {
                log.warn("Invalid password for user: {}. {} attempts remaining", 
                    maskIdentifier(identifier), remainingAttempts);
                throw new UnauthorizedException("Invalid email or password. " + remainingAttempts + " attempts remaining.");
            }
            throw new UnauthorizedException("Account has been locked due to too many failed attempts. Please try again later.");
        }

        // Password correct - reset attempts and check email verification
        loginAttemptService.resetAttempts(identifier);

        if (!user.isEmailVerified()) {
            throw new BusinessException("Please verify your email before logging in");
        }

        log.info("User {} logged in successfully", maskIdentifier(identifier));
        return buildTokenResponse(user);
    }

    // ── Refresh Token ───────────────────────────

    @Override
    @Transactional(readOnly = true)
    public AuthTokenResponseDto refreshToken(RefreshTokenRequestDto cmd) {
        RefreshToken newToken = authDomainService.rotateRefreshToken(
                cmd.getRefreshToken(), refreshTokenExpiryDays);

        User user = authDomainService.findById(newToken.getUserId());

        List<String> roleCodes = user.getRoles().stream()
                .map(r -> r.getCode())
                .toList();

        // If user has MEMBER role, lookup their org context
        UUID organizationId = null;
        String organizationName = null;
        String organizationRole = null;
        if (roleCodes.contains("MEMBER")) {
            organizationId = organizationMemberRepository.findOrganizationIdByUserId(user.getId());
            organizationRole = organizationMemberRepository.findOrgRoleByUserId(user.getId());
            if (organizationId != null) {
                organizationName = organizationRepository.findById(organizationId)
                        .map(Organization::getName)
                        .orElse(null);
            }
        }

        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), roleCodes, organizationId);

        return AuthTokenResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(newToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .user(toUserResponse(user, organizationId, organizationName, organizationRole))
                .build();
    }

    // ── Email Verification ──────────────────────

    @Override
    public void verifyEmail(String token) {
        authDomainService.verifyEmail(token);
        log.info("Email verified via token");
    }

    @Override
    public void resendVerificationEmail(String email) {
        EmailVerificationToken token =
                authDomainService.resendVerificationToken(email, verificationTokenExpiryMinutes);

        User user = authDomainService.findByEmail(email);
        emailService.sendVerificationEmail(user.getEmail(), user.getUsername(), token.getToken());
        log.info("Resent verification email to {}", email);
    }

    // ── Password Reset ──────────────────────────

    @Override
    public void forgotPassword(String email) {
        var user = authDomainService.findByEmail(email);
        var token = authDomainService.createPasswordResetTokenByEmail(email, passwordResetTokenExpiryMinutes);

        emailService.sendPasswordResetEmail(email,user.getFullName(), token.getToken());
        log.info("Password reset email sent to {}", email);
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        String hashedPassword = passwordEncoder.encode(newPassword);
        User user = authDomainService.verifyPasswordResetToken(token);
        authDomainService.resetPassword(user.getId(), hashedPassword);
        log.info("Password reset completed for user {}", user.getEmail());
    }

    // ── Logout ──────────────────────────────────

    @Override
    public void logout(String userId) {
        authDomainService.revokeAllTokens(UUID.fromString(userId));
        log.info("User {} logged out — all refresh tokens revoked", userId);
    }

    // ── Helpers ─────────────────────────────────

    @Transactional(readOnly = true)
    private AuthTokenResponseDto buildTokenResponse(User user) {
        List<String> roleCodes = user.getRoles().stream()
                .map(r -> r.getCode())
                .toList();

        // If user has MEMBER role, lookup their org context
        UUID organizationId = null;
        String organizationName = null;
        String organizationRole = null;
        if (roleCodes.contains("MEMBER")) {
            organizationId = organizationMemberRepository.findOrganizationIdByUserId(user.getId());
            organizationRole = organizationMemberRepository.findOrgRoleByUserId(user.getId());
            if (organizationId != null) {
                organizationName = organizationRepository.findById(organizationId)
                        .map(Organization::getName)
                        .orElse(null);
            }
            log.debug("User {} org context: orgId={}, role={}", user.getId(), organizationId, organizationRole);
        }

        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), roleCodes, organizationId);

        RefreshToken refreshToken = authDomainService.createRefreshToken(
                user.getId(), refreshTokenExpiryDays);

        return AuthTokenResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .user(toUserResponse(user, organizationId, organizationName, organizationRole))
                .build();
    }

    private UserResponseDto toUserResponse(User user, UUID organizationId, String organizationName, String organizationRole) {
        return UserResponseDto.builder()
                .userId(user.getId())
                .organizationId(organizationId)
                .organizationName(organizationName)
                .organizationRole(organizationRole)
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .phone(user.getPhone())
                .gender(user.getGender())
                .streetAddress(user.getStreetAddress())
                .wardName(user.getWardName())
                .provinceName(user.getProvinceName())
                .status(user.getStatus().name())
                .emailVerified(user.isEmailVerified())
                .roles(user.getRoles().stream().map(r -> r.getCode()).toList())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    /**
     * Mask identifier for logging (security: don't log full email/username).
     */
    private String maskIdentifier(String identifier) {
        if (identifier == null || identifier.length() < 3) {
            return "***";
        }
        int atIndex = identifier.indexOf('@');
        if (atIndex > 1) {
            // Email: show first char and domain
            return identifier.charAt(0) + "***@" + identifier.substring(atIndex + 1);
        }
        // Username: show first and last char
        return identifier.charAt(0) + "***" + identifier.charAt(identifier.length() - 1);
    }
}
