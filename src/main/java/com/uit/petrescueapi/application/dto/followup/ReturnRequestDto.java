package com.uit.petrescueapi.application.dto.followup;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Why the adopter cannot keep the pet. A reason code lets the numbers be counted; the note is free. */
public record ReturnRequestDto(
        @NotNull @Pattern(regexp = "BEHAVIOR|PET_CONFLICT|CHILDREN|HOUSING_MOVING|ALLERGY|FINANCIAL|HEALTH_OWNER|TIME|HEALTH_PET|OTHER") String reasonCode,
        @Size(max = 1000) String note) {
}
