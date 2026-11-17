package com.uit.petrescueapi.application.dto.rescue;

import com.uit.petrescueapi.domain.valueobject.PetSize;
import com.uit.petrescueapi.application.support.CanonicalEnum;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import lombok.*;
import com.uit.petrescueapi.validation.Phone;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a rescue case.
 * Includes both codes and names for location fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRescueCaseRequestDto {
    @io.swagger.v3.oas.annotations.media.Schema(description = "RESCUE (default), LOST or FOUND. Cannot be changed by an update.")
    private com.uit.petrescueapi.domain.valueobject.RescueCaseType caseType;
    @io.swagger.v3.oas.annotations.media.Schema(description = "Required for FOUND: ON_STREET, AT_HOME or AT_ORGANIZATION")
    private com.uit.petrescueapi.domain.valueobject.HoldingStatus holdingStatus;
    @io.swagger.v3.oas.annotations.media.Schema(description = "LOST: when the pet was last seen; not in the future")
    private java.time.LocalDateTime lastSeenAt;
    @jakarta.validation.constraints.Size(max = 500)
    @io.swagger.v3.oas.annotations.media.Schema(example = "vòng cổ đỏ, cụt đuôi")
    private String distinctiveMarks;
    private String species;
    private String color;
    @CanonicalEnum(PetSize.class)
    private String size;
    private RescuePriority priority;
    private String description;
    private Double latitude;
    private Double longitude;
    private String locationText;
    private String provinceCode;
    private String provinceName;
    private String wardCode;
    private String wardName;
    private List<String> imageUrls;

    @Builder.Default
    private List<UUID> mediaIds = java.util.Collections.emptyList();

    @Phone
    private String contactPhone;

    /** True also publishes a community post about this case, so people scrolling the feed see it. */
    private boolean shareAsPost;

    /** Text of that post. When empty one is written from the species, the place and the description. */
    @jakarta.validation.constraints.Size(max = 2000)
    private String postContent;
}
