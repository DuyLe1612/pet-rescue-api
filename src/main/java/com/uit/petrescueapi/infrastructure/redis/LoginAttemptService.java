package com.uit.petrescueapi.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

/**
 * Redis-backed service for tracking failed login attempts.
 * 
 * <p>Implements the following security rules:
 * <ul>
 *   <li>Track failed attempts per user identifier (email/username)</li>
 *   <li>Lock account after 5 consecutive failed attempts</li>
 *   <li>Auto-unlock after 30 minutes</li>
 *   <li>Reset counter on successful login</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LoginAttemptService {

    private final StringRedisTemplate redis;

    @Value("${app.security.login.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.security.login.lockout-duration-minutes:30}")
    private int lockoutDurationMinutes;

    @Value("${app.security.login.attempt-ttl-minutes:30}")
    private int attemptTtlMinutes;

    private static final String ATTEMPT_KEY_PREFIX = "login:attempts:";
    private static final String LOCK_KEY_PREFIX = "login:lock:";

    /**
     * Record a failed login attempt and return the current attempt count.
     * 
     * @param identifier Email or username used for login
     * @return Current number of failed attempts
     */
    public int recordFailedAttempt(String identifier) {
        String attemptKey = ATTEMPT_KEY_PREFIX + normalizeKey(identifier);
        String lockKey = LOCK_KEY_PREFIX + normalizeKey(identifier);
        
        // Increment failed attempts counter
        Long attempts = redis.opsForValue().increment(attemptKey);
        int currentAttempts = attempts != null ? attempts.intValue() : 1;
        
        // Set TTL on first attempt
        if (currentAttempts == 1) {
            redis.expire(attemptKey, Duration.ofMinutes(attemptTtlMinutes));
        }
        
        // Check if we should lock the account
        if (currentAttempts >= maxAttempts) {
            lockAccount(identifier);
        }
        
        log.warn("Failed login attempt #{} for user: {}", currentAttempts, maskIdentifier(identifier));
        return currentAttempts;
    }

    /**
     * Reset failed attempts counter for a user after successful login.
     * 
     * @param identifier Email or username
     */
    public void resetAttempts(String identifier) {
        String attemptKey = ATTEMPT_KEY_PREFIX + normalizeKey(identifier);
        String lockKey = LOCK_KEY_PREFIX + normalizeKey(identifier);
        
        redis.delete(attemptKey);
        redis.delete(lockKey);
        
        log.debug("Reset login attempts for user: {}", maskIdentifier(identifier));
    }

    /**
     * Check if the account is currently locked.
     * 
     * @param identifier Email or username
     * @return true if account is locked
     */
    public boolean isLocked(String identifier) {
        String lockKey = LOCK_KEY_PREFIX + normalizeKey(identifier);
        return Boolean.TRUE.equals(redis.hasKey(lockKey));
    }

    /**
     * Get the remaining lockout time in seconds.
     * 
     * @param identifier Email or username
     * @return Remaining seconds until unlock, or -1 if not locked
     */
    public long getRemainingLockoutSeconds(String identifier) {
        String lockKey = LOCK_KEY_PREFIX + normalizeKey(identifier);
        Long ttl = redis.getExpire(lockKey);
        return ttl != null && ttl > 0 ? ttl : -1;
    }

    /**
     * Get current failed attempt count for a user.
     * 
     * @param identifier Email or username
     * @return Current number of failed attempts
     */
    public int getAttemptCount(String identifier) {
        String attemptKey = ATTEMPT_KEY_PREFIX + normalizeKey(identifier);
        String count = redis.opsForValue().get(attemptKey);
        return count != null ? Integer.parseInt(count) : 0;
    }

    /**
     * Check if user should be rate-limited based on recent attempts.
     * 
     * @param identifier Email or username
     * @return true if too many recent attempts detected
     */
    public boolean isRateLimited(String identifier) {
        return getAttemptCount(identifier) >= maxAttempts;
    }

    /**
     * Lock the account in Redis.
     */
    private void lockAccount(String identifier) {
        String lockKey = LOCK_KEY_PREFIX + normalizeKey(identifier);
        redis.opsForValue().set(lockKey, "locked", Duration.ofMinutes(lockoutDurationMinutes));
        log.warn("Account locked due to {} failed attempts: {}", maxAttempts, maskIdentifier(identifier));
    }

    /**
     * Normalize the identifier for consistent Redis key generation.
     */
    private String normalizeKey(String identifier) {
        return identifier.toLowerCase().trim();
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
