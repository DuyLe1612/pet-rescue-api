package com.uit.petrescueapi.infrastructure.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LoginAttemptServiceTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        service = new LoginAttemptService(redisTemplate);
        ReflectionTestUtils.setField(service, "maxAttempts", 5);
        ReflectionTestUtils.setField(service, "lockoutDurationMinutes", 30);
        ReflectionTestUtils.setField(service, "attemptTtlMinutes", 30);
    }

    @Test
    @DisplayName("First failed attempt returns 1 and sets TTL")
    void recordFailedAttempt_FirstAttempt_ReturnsOneAndSetsTTL() {
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);

        int attempts = service.recordFailedAttempt("test@example.com");

        assertThat(attempts).isEqualTo(1);
        verify(redisTemplate).expire(eq("login:attempts:test@example.com"), eq(Duration.ofMinutes(30)));
    }

    @Test
    @DisplayName("Fifth failed attempt locks account")
    void recordFailedAttempt_FifthAttempt_LocksAccount() {
        when(valueOperations.increment(anyString())).thenReturn(5L);
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);
        doNothing().when(valueOperations).set(anyString(), anyString(), any(Duration.class));

        int attempts = service.recordFailedAttempt("test@example.com");

        assertThat(attempts).isEqualTo(5);
        verify(valueOperations).set(eq("login:lock:test@example.com"), eq("locked"), eq(Duration.ofMinutes(30)));
    }

    @Test
    @DisplayName("Reset attempts deletes both keys")
    void resetAttempts_DeletesBothKeys() {
        when(redisTemplate.delete(anyString())).thenReturn(true);

        service.resetAttempts("test@example.com");

        verify(redisTemplate).delete("login:attempts:test@example.com");
        verify(redisTemplate).delete("login:lock:test@example.com");
    }

    @Test
    @DisplayName("isLocked returns true when lock key exists")
    void isLocked_WhenLockExists_ReturnsTrue() {
        when(redisTemplate.hasKey("login:lock:test@example.com")).thenReturn(true);

        boolean locked = service.isLocked("test@example.com");

        assertThat(locked).isTrue();
    }

    @Test
    @DisplayName("isLocked returns false when lock key does not exist")
    void isLocked_WhenLockNotExists_ReturnsFalse() {
        when(redisTemplate.hasKey("login:lock:test@example.com")).thenReturn(false);

        boolean locked = service.isLocked("test@example.com");

        assertThat(locked).isFalse();
    }

    @Test
    @DisplayName("getRemainingLockoutSeconds returns TTL when key exists")
    void getRemainingLockoutSeconds_WhenLocked_ReturnsTTL() {
        when(redisTemplate.getExpire("login:lock:test@example.com")).thenReturn(1500L);

        long seconds = service.getRemainingLockoutSeconds("test@example.com");

        assertThat(seconds).isEqualTo(1500L);
    }

    @Test
    @DisplayName("getRemainingLockoutSeconds returns -1 when not locked")
    void getRemainingLockoutSeconds_WhenNotLocked_ReturnsNegativeOne() {
        when(redisTemplate.getExpire("login:lock:test@example.com")).thenReturn(-2L);

        long seconds = service.getRemainingLockoutSeconds("test@example.com");

        assertThat(seconds).isEqualTo(-1L);
    }

    @Test
    @DisplayName("getAttemptCount returns stored count")
    void getAttemptCount_ReturnsStoredCount() {
        when(valueOperations.get("login:attempts:test@example.com")).thenReturn("3");

        int count = service.getAttemptCount("test@example.com");

        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("getAttemptCount returns 0 when no attempts")
    void getAttemptCount_WhenNoAttempts_ReturnsZero() {
        when(valueOperations.get(anyString())).thenReturn(null);

        int count = service.getAttemptCount("test@example.com");

        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("isRateLimited returns true when max attempts reached")
    void isRateLimited_WhenMaxAttempts_ReturnsTrue() {
        when(valueOperations.get("login:attempts:test@example.com")).thenReturn("5");

        boolean rateLimited = service.isRateLimited("test@example.com");

        assertThat(rateLimited).isTrue();
    }

    @Test
    @DisplayName("isRateLimited returns false when below max attempts")
    void isRateLimited_WhenBelowMax_ReturnsFalse() {
        when(valueOperations.get("login:attempts:test@example.com")).thenReturn("3");

        boolean rateLimited = service.isRateLimited("test@example.com");

        assertThat(rateLimited).isFalse();
    }

    @Test
    @DisplayName("isRateLimited returns false when no attempts recorded")
    void isRateLimited_WhenNoAttempts_ReturnsFalse() {
        when(valueOperations.get(anyString())).thenReturn(null);

        boolean rateLimited = service.isRateLimited("test@example.com");

        assertThat(rateLimited).isFalse();
    }

    @Test
    @DisplayName("Identifier is normalized to lowercase for keys")
    void recordFailedAttempt_NormalizesIdentifier() {
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);

        service.recordFailedAttempt("TEST@EXAMPLE.COM");

        verify(valueOperations).increment("login:attempts:test@example.com");
    }
}
