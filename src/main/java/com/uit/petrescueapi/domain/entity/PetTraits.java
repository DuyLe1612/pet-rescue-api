package com.uit.petrescueapi.domain.entity;

import com.uit.petrescueapi.domain.valueobject.BiteHistory;
import com.uit.petrescueapi.domain.valueobject.CareLevel;
import com.uit.petrescueapi.domain.valueobject.PetSize;
import com.uit.petrescueapi.domain.valueobject.TraitSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * What the people caring for a pet have recorded about it. Scales run 1 to 5; null means nobody
 * recorded it yet. Aggression and food guarding are management notes ({@code note}), not scores.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PetTraits {
    private UUID petId;
    private Integer friendlyPeople;
    private Integer friendlyChildren;
    private Integer fearStrangers;
    private Integer fearNoise;
    private Integer energy;
    private Integer goodWithDogs;
    private Integer goodWithCats;
    /** Longest time the pet copes alone. */
    private Integer aloneMinutes;
    private Integer houseTrained;
    private BiteHistory biteHistory;
    private boolean biteVerified;
    private PetSize size;
    private Integer noiseLevel;
    /** Typical monthly cost, same currency unit as the adopter's budget. */
    private Integer monthlyCost;
    private CareLevel careLevel;
    private TraitSource sourceRole;
    private String note;
    private UUID updatedBy;
    private LocalDateTime updatedAt;

    /** A pet nobody has described yet. */
    public static PetTraits unknown(UUID petId) {
        return PetTraits.builder().petId(petId).build();
    }
}
