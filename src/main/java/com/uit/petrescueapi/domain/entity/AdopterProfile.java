package com.uit.petrescueapi.domain.entity;

import com.uit.petrescueapi.domain.valueobject.BuildingPetRule;
import com.uit.petrescueapi.domain.valueobject.HousingCondition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * What an adopter declares about their home, once. Used to match and to advise, never to screen
 * people out. Experience and willingness to train are for the organization to read; they do not
 * lower a score.
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class AdopterProfile {
    private UUID userId;
    private HousingCondition housingType;
    private BuildingPetRule buildingRule;
    private boolean hasChildrenUnder5;
    private boolean hasChildren5To8;
    private boolean hasChildrenOver8;
    private boolean allergyDog;
    private boolean allergyCat;
    private boolean hasDog;
    private boolean hasCat;
    private Integer hoursAwayPerDay;
    /** 1 (quiet) to 5 (very active). */
    private Integer activityLevel;
    private Integer monthlyBudget;
    private Integer emergencyVetBudget;
    /** How many weeks the adopter expects the pet to need to settle in. */
    private Integer adjustmentWeeksExpected;
    private String intolerableBehaviors;
    private String returnCircumstances;
    private String relocationPlan;
    private Integer experienceLevel;
    private Boolean willingTraining;
    private String provinceCode;
    private LocalDateTime consentAt;
    private int version;
    private LocalDateTime updatedAt;

    public boolean hasYoungChildren() {
        return hasChildrenUnder5 || hasChildren5To8;
    }

    public boolean hasChildren() {
        return hasYoungChildren() || hasChildrenOver8;
    }
}
