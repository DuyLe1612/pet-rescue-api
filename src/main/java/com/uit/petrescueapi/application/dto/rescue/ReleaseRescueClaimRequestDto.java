package com.uit.petrescueapi.application.dto.rescue;

import com.uit.petrescueapi.domain.valueobject.RescueClaimReleaseReason;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseRescueClaimRequestDto {

    @Schema(description = "GAVE_UP (default), NEED_HELP flags the case in the queue, NOT_FOUND tells the reporter the animal was not there")
    private RescueClaimReleaseReason reason;
}
