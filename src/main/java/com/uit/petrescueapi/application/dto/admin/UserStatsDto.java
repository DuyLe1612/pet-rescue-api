package com.uit.petrescueapi.application.dto.admin;

public record UserStatsDto(
        long total,
        long active,
        long pendingVerification,
        long banned
) {
}
