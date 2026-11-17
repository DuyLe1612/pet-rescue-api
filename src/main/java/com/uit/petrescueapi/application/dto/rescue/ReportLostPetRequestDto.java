package com.uit.petrescueapi.application.dto.rescue;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Where and when the pet was last seen. Species and colour come from the pet's profile. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Report one of my pets lost")
public class ReportLostPetRequestDto {

    @NotNull
    @DecimalMin("-90") @DecimalMax("90")
    private Double latitude;

    @NotNull
    @DecimalMin("-180") @DecimalMax("180")
    private Double longitude;

    @Size(max = 500)
    private String locationText;
    private String provinceCode;
    private String provinceName;
    private String wardCode;
    private String wardName;

    @Schema(description = "Not in the future; defaults to now")
    private LocalDateTime lastSeenAt;

    @Size(max = 500)
    @Schema(example = "vòng cổ đỏ, cụt đuôi")
    private String distinctiveMarks;

    @Size(max = 2000)
    private String description;

    private String contactPhone;
}
