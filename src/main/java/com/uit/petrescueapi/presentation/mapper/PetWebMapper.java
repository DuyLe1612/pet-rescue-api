package com.uit.petrescueapi.presentation.mapper;

import com.uit.petrescueapi.application.dto.pet.PetResponseDto;
import com.uit.petrescueapi.application.dto.pet.PetSummaryResponseDto;
import com.uit.petrescueapi.domain.entity.Pet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper: domain {@link Pet} → presentation DTOs.
 * <ul>
 *   <li>{@link PetResponseDto} — full detail / write response</li>
 *   <li>{@link PetSummaryResponseDto} — lightweight list response</li>
 * </ul>
 */
@Mapper(componentModel = "spring")
public interface PetWebMapper {

    // ── Full response (detail + write) ──────────

    PetResponseDto toDto(Pet pet);

    List<PetResponseDto> toDtoList(List<Pet> pets);

    // ── Summary response (list views) ───────────


    PetSummaryResponseDto toSummaryDto(Pet pet);

    List<PetSummaryResponseDto> toSummaryDtoList(List<Pet> pets);

    // ── Helpers ─────────────────────────────────
    /** Extract first image URL for thumbnail display. */
    default String firstImage(List<String> urls) {
        return (urls != null && !urls.isEmpty()) ? urls.getFirst() : null;
    }
}
