package com.uit.petrescueapi.application.dto.rescue;

import com.uit.petrescueapi.domain.valueobject.RescueCaseFlagReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Telling the community a case may need no more help")
public class FlagRescueCaseRequestDto {

    @NotNull
    @Schema(example = "ALREADY_HANDLED")
    private RescueCaseFlagReason reason;

    @Size(max = 500)
    private String note;
}
