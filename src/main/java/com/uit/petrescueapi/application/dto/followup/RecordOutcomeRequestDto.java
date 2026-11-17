package com.uit.petrescueapi.application.dto.followup;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RecordOutcomeRequestDto(
        @NotNull @Pattern(regexp = "RETURNED_BY_ADOPTER|REHOMED|LOST|DECEASED") String outcomeType,
        @Pattern(regexp = "BEHAVIOR|PET_CONFLICT|CHILDREN|HOUSING_MOVING|ALLERGY|FINANCIAL|HEALTH_OWNER|TIME|HEALTH_PET|OTHER") String reasonCode,
        @Size(max = 1000) String note) {
}
