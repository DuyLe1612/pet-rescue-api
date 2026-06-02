package com.uit.petrescueapi.application.dto.admin;

public record PetStatsDto(
        long total,
        long available,
        long pending,
        long adopted
) {
}
