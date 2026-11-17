package com.uit.petrescueapi.application.dto.followup;

import jakarta.validation.constraints.Size;

public record SupportRequestDto(@Size(max = 1000) String note) {
}
