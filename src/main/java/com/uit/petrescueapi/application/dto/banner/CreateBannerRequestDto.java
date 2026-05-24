package com.uit.petrescueapi.application.dto.banner;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBannerRequestDto {

    @NotBlank(message = "Title is required")
    @Size(max = 200)
    private String title;

    @Size(max = 500)
    private String subtitle;

    @Size(max = 100)
    private String buttonText;

    private UUID mediaId;

    @Size(max = 500)
    private String linkUrl;

    @Size(max = 20)
    private String linkType;  // INTERNAL, EXTERNAL, NONE

    @Min(0)
    private Integer displayOrder;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Builder.Default
    private boolean active = true;

    private String targetPage;  // HOME, ADOPTION, RESCUE, etc.
}
