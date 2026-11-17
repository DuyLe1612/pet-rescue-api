package com.uit.petrescueapi.application.dto.matching;

import com.uit.petrescueapi.domain.entity.AdopterProfile;
import com.uit.petrescueapi.domain.valueobject.BuildingPetRule;
import com.uit.petrescueapi.domain.valueobject.HousingCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** The adopter profile as sent and returned. Short on purpose: a long form is a form nobody fills in. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Adopter profile used for matching. Read by the adopter and by organizations reviewing their applications.")
public class AdopterProfileDto {

    @NotNull
    private HousingCondition housingType;

    @Schema(description = "What the building or landlord allows. Defaults to UNKNOWN.")
    private BuildingPetRule buildingRule;

    private boolean hasChildrenUnder5;
    private boolean hasChildren5To8;
    private boolean hasChildrenOver8;
    private boolean allergyDog;
    private boolean allergyCat;
    private boolean hasDog;
    private boolean hasCat;

    @Min(0) @Max(24)
    private Integer hoursAwayPerDay;

    @Min(1) @Max(5)
    @Schema(description = "1 quiet, 5 very active")
    private Integer activityLevel;

    @Min(0)
    private Integer monthlyBudget;

    @Min(0)
    private Integer emergencyVetBudget;

    @Min(0) @Max(104)
    @Schema(description = "Weeks the adopter expects the pet to need to settle in")
    private Integer adjustmentWeeksExpected;

    @Size(max = 1000)
    private String intolerableBehaviors;

    @Size(max = 1000)
    private String returnCircumstances;

    @Size(max = 1000)
    private String relocationPlan;

    @Min(1) @Max(5)
    @Schema(description = "For the organization to read. Does not change the score.")
    private Integer experienceLevel;

    private Boolean willingTraining;

    @Size(max = 20)
    private String provinceCode;

    @Schema(description = "Must be true when saving: the adopter agrees that organizations they apply to can read this profile.")
    private boolean consent;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private Integer version;

    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime updatedAt;

    public AdopterProfile toDomain() {
        return AdopterProfile.builder()
                .housingType(housingType)
                .buildingRule(buildingRule == null ? BuildingPetRule.UNKNOWN : buildingRule)
                .hasChildrenUnder5(hasChildrenUnder5)
                .hasChildren5To8(hasChildren5To8)
                .hasChildrenOver8(hasChildrenOver8)
                .allergyDog(allergyDog)
                .allergyCat(allergyCat)
                .hasDog(hasDog)
                .hasCat(hasCat)
                .hoursAwayPerDay(hoursAwayPerDay)
                .activityLevel(activityLevel)
                .monthlyBudget(monthlyBudget)
                .emergencyVetBudget(emergencyVetBudget)
                .adjustmentWeeksExpected(adjustmentWeeksExpected)
                .intolerableBehaviors(intolerableBehaviors)
                .returnCircumstances(returnCircumstances)
                .relocationPlan(relocationPlan)
                .experienceLevel(experienceLevel)
                .willingTraining(willingTraining)
                .provinceCode(provinceCode)
                .build();
    }

    public static AdopterProfileDto from(AdopterProfile p) {
        return AdopterProfileDto.builder()
                .housingType(p.getHousingType())
                .buildingRule(p.getBuildingRule())
                .hasChildrenUnder5(p.isHasChildrenUnder5())
                .hasChildren5To8(p.isHasChildren5To8())
                .hasChildrenOver8(p.isHasChildrenOver8())
                .allergyDog(p.isAllergyDog())
                .allergyCat(p.isAllergyCat())
                .hasDog(p.isHasDog())
                .hasCat(p.isHasCat())
                .hoursAwayPerDay(p.getHoursAwayPerDay())
                .activityLevel(p.getActivityLevel())
                .monthlyBudget(p.getMonthlyBudget())
                .emergencyVetBudget(p.getEmergencyVetBudget())
                .adjustmentWeeksExpected(p.getAdjustmentWeeksExpected())
                .intolerableBehaviors(p.getIntolerableBehaviors())
                .returnCircumstances(p.getReturnCircumstances())
                .relocationPlan(p.getRelocationPlan())
                .experienceLevel(p.getExperienceLevel())
                .willingTraining(p.getWillingTraining())
                .provinceCode(p.getProvinceCode())
                .consent(p.getConsentAt() != null)
                .version(p.getVersion())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
