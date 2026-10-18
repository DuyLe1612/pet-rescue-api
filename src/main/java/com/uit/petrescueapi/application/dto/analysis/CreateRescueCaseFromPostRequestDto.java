package com.uit.petrescueapi.application.dto.analysis;

import io.swagger.v3.oas.annotations.media.Schema;

/** What a free-text post cannot provide; everything else is prefilled from the confirmed facets. */
@Schema(description = "Extra data for creating a Rescue Case from a confirmed post")
public record CreateRescueCaseFromPostRequestDto(Double latitude, Double longitude, String contactPhone) {
}
