package com.uit.petrescueapi.application.dto.rescue;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "A progress note on a rescue case")
public class CreateRescueCaseUpdateRequestDto {

    @NotBlank
    @Size(max = 2000)
    @Schema(example = "Đã tới nơi, bé đang ở gầm cầu")
    private String note;

    @Size(max = 5)
    @Schema(description = "Your own uploads, at most 5")
    private List<UUID> mediaIds;
}
