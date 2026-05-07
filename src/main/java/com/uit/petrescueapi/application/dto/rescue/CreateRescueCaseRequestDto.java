package com.uit.petrescueapi.application.dto.rescue;

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
    private UUID petId;
    private UUID organizationId;
    private String species;
    private String color;
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
    @Phone
    private String contactPhone;
}
