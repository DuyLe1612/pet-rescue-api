package com.uit.petrescueapi.application.dto.matching;

import com.uit.petrescueapi.domain.entity.PetTraits;
import com.uit.petrescueapi.domain.valueobject.BiteHistory;
import com.uit.petrescueapi.domain.valueobject.CareLevel;
import com.uit.petrescueapi.domain.valueobject.PetSize;
import com.uit.petrescueapi.domain.valueobject.TraitSource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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
@Schema(description = "What is known about a pet for matching. Scales are 1-5; leave a field out when nobody observed it.")
public class PetTraitsDto {

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private UUID petId;

    @Min(1) @Max(5) private Integer friendlyPeople;
    @Min(1) @Max(5) private Integer friendlyChildren;
    @Min(1) @Max(5) private Integer fearStrangers;
    @Min(1) @Max(5) private Integer fearNoise;
    @Min(1) @Max(5) private Integer energy;
    @Min(1) @Max(5) private Integer goodWithDogs;
    @Min(1) @Max(5) private Integer goodWithCats;

    @Min(0) @Max(1440)
    @Schema(description = "Longest time the pet copes alone, in minutes")
    private Integer aloneMinutes;

    @Min(1) @Max(5) private Integer houseTrained;

    private BiteHistory biteHistory;

    @Schema(description = "Staff confirmed the bite history. Only a verified INJURY raises a warning.")
    private boolean biteVerified;

    private PetSize size;

    @Min(1) @Max(5) private Integer noiseLevel;

    @Min(0)
    private Integer monthlyCost;

    private CareLevel careLevel;

    @Schema(description = "Who this description comes from. Defaults to STAFF.")
    private TraitSource sourceRole;

    @Size(max = 2000)
    @Schema(description = "Management notes, e.g. food guarding. Not scored.")
    private String note;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    public PetTraits toDomain(UUID petId) {
        return PetTraits.builder()
                .petId(petId)
                .friendlyPeople(friendlyPeople)
                .friendlyChildren(friendlyChildren)
                .fearStrangers(fearStrangers)
                .fearNoise(fearNoise)
                .energy(energy)
                .goodWithDogs(goodWithDogs)
                .goodWithCats(goodWithCats)
                .aloneMinutes(aloneMinutes)
                .houseTrained(houseTrained)
                .biteHistory(biteHistory)
                .biteVerified(biteVerified)
                .size(size)
                .noiseLevel(noiseLevel)
                .monthlyCost(monthlyCost)
                .careLevel(careLevel)
                .sourceRole(sourceRole == null ? TraitSource.STAFF : sourceRole)
                .note(note)
                .build();
    }

    public static PetTraitsDto from(PetTraits t) {
        return PetTraitsDto.builder()
                .petId(t.getPetId())
                .friendlyPeople(t.getFriendlyPeople())
                .friendlyChildren(t.getFriendlyChildren())
                .fearStrangers(t.getFearStrangers())
                .fearNoise(t.getFearNoise())
                .energy(t.getEnergy())
                .goodWithDogs(t.getGoodWithDogs())
                .goodWithCats(t.getGoodWithCats())
                .aloneMinutes(t.getAloneMinutes())
                .houseTrained(t.getHouseTrained())
                .biteHistory(t.getBiteHistory())
                .biteVerified(t.isBiteVerified())
                .size(t.getSize())
                .noiseLevel(t.getNoiseLevel())
                .monthlyCost(t.getMonthlyCost())
                .careLevel(t.getCareLevel())
                .sourceRole(t.getSourceRole())
                .note(t.getNote())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
