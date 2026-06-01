package com.uit.petrescueapi.application.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Lightweight public user info for search.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Public user info for search")
public class UserPublicSearchDto {

    @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID userId;

    @Schema(example = "johndoe")
    private String username;

    @Schema(example = "John Doe")
    private String fullName;

    @Schema(example = "https://cdn.example.com/avatar.jpg")
    private String avatarUrl;
}
