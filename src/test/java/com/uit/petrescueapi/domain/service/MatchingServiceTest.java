package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.AdopterProfile;
import com.uit.petrescueapi.domain.entity.PetTraits;
import com.uit.petrescueapi.domain.valueobject.BiteHistory;
import com.uit.petrescueapi.domain.valueobject.BuildingPetRule;
import com.uit.petrescueapi.domain.valueobject.CareLevel;
import com.uit.petrescueapi.domain.valueobject.HousingCondition;
import com.uit.petrescueapi.domain.valueobject.MatchCertainty;
import com.uit.petrescueapi.domain.valueobject.MatchCriterion;
import com.uit.petrescueapi.domain.valueobject.MatchLevel;
import com.uit.petrescueapi.domain.valueobject.MatchReasonStatus;
import com.uit.petrescueapi.domain.valueobject.MatchResult;
import com.uit.petrescueapi.domain.valueobject.MatchWarningCode;
import com.uit.petrescueapi.domain.valueobject.PetSize;
import com.uit.petrescueapi.domain.valueobject.SpeciesKind;
import com.uit.petrescueapi.domain.valueobject.TraitSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class MatchingServiceTest {

    private final MatchingService matching = new MatchingService();

    /** A home with nothing that could raise a warning and no optional answers. */
    private static AdopterProfile.AdopterProfileBuilder home() {
        return AdopterProfile.builder().housingType(HousingCondition.HOUSE).buildingRule(BuildingPetRule.ALLOWED);
    }

    private static PetTraits.PetTraitsBuilder pet() {
        return PetTraits.builder().sourceRole(TraitSource.STAFF);
    }

    /** A well-described, easy pet. */
    private static PetTraits.PetTraitsBuilder easyPet() {
        return pet().friendlyPeople(5).friendlyChildren(5).fearStrangers(1).fearNoise(1).energy(3)
                .goodWithDogs(5).goodWithCats(5).aloneMinutes(480).size(PetSize.SMALL).noiseLevel(1)
                .monthlyCost(500_000).careLevel(CareLevel.LOW);
    }

    private static AdopterProfile.AdopterProfileBuilder readyHome() {
        return home().hoursAwayPerDay(8).activityLevel(3).monthlyBudget(1_000_000).adjustmentWeeksExpected(4);
    }

    // ── Warnings ────────────────────────────────────

    static Stream<Arguments> warningCases() {
        return Stream.of(
                arguments("no pets allowed", home().buildingRule(BuildingPetRule.NO_PETS).build(), SpeciesKind.OTHER, pet().build(),
                        MatchWarningCode.BUILDING_RULE),
                arguments("no dogs, pet is a dog", home().buildingRule(BuildingPetRule.NO_DOGS).build(), SpeciesKind.DOG, pet().build(),
                        MatchWarningCode.BUILDING_RULE),
                arguments("no cats, pet is a cat", home().buildingRule(BuildingPetRule.NO_CATS).build(), SpeciesKind.CAT, pet().build(),
                        MatchWarningCode.BUILDING_RULE),
                arguments("small only, pet is large", home().buildingRule(BuildingPetRule.SMALL_ONLY).build(), SpeciesKind.DOG,
                        pet().size(PetSize.LARGE).build(), MatchWarningCode.BUILDING_RULE),
                arguments("dog allergy", home().allergyDog(true).build(), SpeciesKind.DOG, pet().build(), MatchWarningCode.ALLERGY),
                arguments("cat allergy", home().allergyCat(true).build(), SpeciesKind.CAT, pet().build(), MatchWarningCode.ALLERGY),
                arguments("toddler, pet clearly bad with children", home().hasChildrenUnder5(true).build(), SpeciesKind.DOG,
                        pet().friendlyChildren(1).build(), MatchWarningCode.NOT_GOOD_WITH_CHILDREN),
                arguments("resident dog, pet clearly bad with dogs", home().hasDog(true).build(), SpeciesKind.CAT,
                        pet().goodWithDogs(1).build(), MatchWarningCode.NOT_GOOD_WITH_DOGS),
                arguments("resident cat, pet clearly bad with cats", home().hasCat(true).build(), SpeciesKind.DOG,
                        pet().goodWithCats(1).build(), MatchWarningCode.NOT_GOOD_WITH_CATS),
                arguments("verified bite injury, child of six", home().hasChildren5To8(true).build(), SpeciesKind.DOG,
                        pet().biteHistory(BiteHistory.INJURY).biteVerified(true).build(), MatchWarningCode.BITE_HISTORY_WITH_CHILD));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("warningCases")
    void eachConstraintRaisesItsWarningAndLeavesNoScore(String name, AdopterProfile adopter, SpeciesKind species,
                                                        PetTraits pet, MatchWarningCode expected) {
        MatchResult result = matching.evaluate(adopter, species, pet);

        assertThat(result.warnings()).extracting(MatchResult.Warning::code).containsExactly(expected);
        assertThat(result.warnings().get(0).text()).isNotBlank();
        assertThat(result.level()).isEqualTo(MatchLevel.NOT_SUITABLE);
        assertThat(result.score()).isNull();
    }

    static Stream<Arguments> noWarningCases() {
        return Stream.of(
                arguments("nothing recorded about the pet, risky home",
                        home().hasChildrenUnder5(true).hasDog(true).hasCat(true).buildingRule(BuildingPetRule.SMALL_ONLY).build(),
                        SpeciesKind.DOG, null),
                arguments("building rule unknown", home().buildingRule(BuildingPetRule.UNKNOWN).build(), SpeciesKind.DOG, pet().build()),
                arguments("dog allergy but the pet is a cat", home().allergyDog(true).build(), SpeciesKind.CAT, pet().build()),
                arguments("no dogs but the pet is a cat", home().buildingRule(BuildingPetRule.NO_DOGS).build(), SpeciesKind.CAT, pet().build()),
                arguments("small only and the pet is small", home().buildingRule(BuildingPetRule.SMALL_ONLY).build(), SpeciesKind.DOG,
                        pet().size(PetSize.SMALL).build()),
                arguments("only a teenager at home", home().hasChildrenOver8(true).build(), SpeciesKind.DOG,
                        pet().friendlyChildren(1).biteHistory(BiteHistory.INJURY).biteVerified(true).build()),
                arguments("bite injury not verified", home().hasChildrenUnder5(true).build(), SpeciesKind.DOG,
                        pet().biteHistory(BiteHistory.INJURY).biteVerified(false).build()),
                arguments("verified but minor bite", home().hasChildrenUnder5(true).build(), SpeciesKind.DOG,
                        pet().biteHistory(BiteHistory.MINOR).biteVerified(true).build()),
                arguments("shy with children, not clearly bad", home().hasChildrenUnder5(true).build(), SpeciesKind.DOG,
                        pet().friendlyChildren(2).build()),
                arguments("bad with dogs but no dog at home", home().build(), SpeciesKind.DOG, pet().goodWithDogs(1).build()),
                // no yard and little experience were hard constraints in the proposal; they are not any more
                arguments("flat, first-time adopter", home().housingType(HousingCondition.APARTMENT).experienceLevel(1).build(),
                        SpeciesKind.DOG, pet().size(PetSize.LARGE).build()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("noWarningCases")
    void unknownOrUnrelatedFactsNeverRaiseAWarning(String name, AdopterProfile adopter, SpeciesKind species, PetTraits pet) {
        MatchResult result = matching.evaluate(adopter, species, pet);

        assertThat(result.warnings()).isEmpty();
        assertThat(result.level()).isNotEqualTo(MatchLevel.NOT_SUITABLE);
        assertThat(result.score()).isNotNull();
    }

    @Test
    void severalConstraintsAreAllReported() {
        MatchResult result = matching.evaluate(
                home().allergyDog(true).hasChildrenUnder5(true).hasCat(true).build(), SpeciesKind.DOG,
                pet().friendlyChildren(1).goodWithCats(1).build());

        assertThat(result.warnings()).extracting(MatchResult.Warning::code).containsExactly(
                MatchWarningCode.ALLERGY, MatchWarningCode.NOT_GOOD_WITH_CHILDREN, MatchWarningCode.NOT_GOOD_WITH_CATS);
    }

    // ── Score ───────────────────────────────────────

    @Test
    void weightsAddUpToOneHundred() {
        assertThat(Arrays.stream(MatchCriterion.values()).mapToInt(MatchCriterion::weight).sum()).isEqualTo(100);
    }

    @Test
    void everyCriterionHasAReasonInAFixedOrder() {
        MatchResult result = matching.evaluate(readyHome().build(), SpeciesKind.DOG, easyPet().build());

        assertThat(result.reasons()).extracting(MatchResult.Reason::code).containsExactly(MatchCriterion.values());
        assertThat(result.reasons()).allSatisfy(reason -> {
            assertThat(reason.text()).isNotBlank();
            assertThat(reason.weight()).isEqualTo(reason.code().weight());
        });
        assertThat(result.weightsVersion()).isEqualTo(MatchingService.WEIGHTS_VERSION);
    }

    @Test
    void zeroCertaintyGivesExactlyTheNeutralValue() {
        assertThat(MatchingService.adjust(0.0, 0)).isEqualTo(0.5);
        assertThat(MatchingService.adjust(1.0, 0)).isEqualTo(0.5);
        assertThat(MatchingService.adjust(1.0, 1)).isEqualTo(1.0);
        assertThat(MatchingService.adjust(0.0, 0.4)).isEqualTo(0.3);
    }

    @Test
    void aPetNobodyDescribedScoresInTheMiddleNotAtTheBottom() {
        // a resident dog, so even "existing pets" depends on facts nobody recorded
        MatchResult result = matching.evaluate(home().hasDog(true).build(), SpeciesKind.DOG, null);

        assertThat(result.score()).isEqualTo(50.0);
        assertThat(result.level()).isEqualTo(MatchLevel.MEDIUM);
        assertThat(result.certainty()).isEqualTo(MatchCertainty.LOW);
        assertThat(result.reasons()).allSatisfy(reason -> {
            assertThat(reason.status()).isEqualTo(MatchReasonStatus.UNKNOWN);
            assertThat(reason.certainty()).isEqualTo(MatchCertainty.NONE);
        });
    }

    @Test
    void aWellDescribedGoodFitIsHighAndABadFitIsLow() {
        MatchResult good = matching.evaluate(readyHome().build(), SpeciesKind.DOG, easyPet().sourceRole(TraitSource.FOSTER).build());
        MatchResult bad = matching.evaluate(
                readyHome().housingType(HousingCondition.APARTMENT).hoursAwayPerDay(12).activityLevel(1).monthlyBudget(100_000)
                        .adjustmentWeeksExpected(0).hasDog(true).build(),
                SpeciesKind.DOG,
                pet().sourceRole(TraitSource.FOSTER).friendlyPeople(2).fearStrangers(5).energy(5).aloneMinutes(30)
                        .size(PetSize.LARGE).noiseLevel(5).goodWithDogs(2).monthlyCost(2_000_000).careLevel(CareLevel.HIGH).build());

        assertThat(good.level()).isEqualTo(MatchLevel.HIGH);
        assertThat(good.score()).isEqualTo(100.0);
        assertThat(good.certainty()).isEqualTo(MatchCertainty.HIGH);
        assertThat(bad.level()).isEqualTo(MatchLevel.LOW);
        assertThat(bad.warnings()).isEmpty();
        assertThat(bad.reasons()).extracting(MatchResult.Reason::status).containsOnly(MatchReasonStatus.WARN);
    }

    @Test
    void theLessSureTheSourceTheCloserTheScoreStaysToNeutral() {
        AdopterProfile adopter = readyHome().build();
        double foster = matching.evaluate(adopter, SpeciesKind.DOG, easyPet().sourceRole(TraitSource.FOSTER).build()).score();
        double staff = matching.evaluate(adopter, SpeciesKind.DOG, easyPet().sourceRole(TraitSource.STAFF).build()).score();
        double hearsay = matching.evaluate(adopter, SpeciesKind.DOG, easyPet().sourceRole(TraitSource.PREVIOUS_OWNER).build()).score();

        assertThat(foster).isGreaterThan(staff);
        assertThat(staff).isGreaterThan(hearsay);
        assertThat(hearsay).isGreaterThan(50.0);
    }

    @Test
    void hearsayIsNamedInTheReason() {
        MatchResult result = matching.evaluate(readyHome().build(), SpeciesKind.DOG,
                easyPet().sourceRole(TraitSource.PREVIOUS_OWNER).build());

        MatchResult.Reason lifestyle = result.reasons().get(0);
        assertThat(lifestyle.certainty()).isEqualTo(MatchCertainty.LOW);
        assertThat(lifestyle.text()).contains("lời kể");
    }

    @Test
    void littleExperienceDoesNotLowerTheScore() {
        PetTraits pet = easyPet().careLevel(CareLevel.HIGH).build();
        MatchResult novice = matching.evaluate(readyHome().experienceLevel(1).willingTraining(false).build(), SpeciesKind.DOG, pet);
        MatchResult veteran = matching.evaluate(readyHome().experienceLevel(5).willingTraining(true).build(), SpeciesKind.DOG, pet);

        assertThat(novice).isEqualTo(veteran);
    }

    @Test
    void sameInputAlwaysGivesTheSameResult() {
        AdopterProfile adopter = readyHome().hasCat(true).hasChildren5To8(true).build();
        PetTraits pet = easyPet().goodWithCats(2).fearNoise(4).build();

        MatchResult first = matching.evaluate(adopter, SpeciesKind.DOG, pet);
        for (int i = 0; i < 20; i++) {
            assertThat(matching.evaluate(adopter, SpeciesKind.DOG, pet)).isEqualTo(first);
        }
    }

    @Test
    void aWarningStillComesWithReasonsAndPreparation() {
        MatchResult result = matching.evaluate(readyHome().allergyDog(true).hasCat(true).build(), SpeciesKind.DOG, easyPet().build());

        assertThat(result.score()).isNull();
        assertThat(result.reasons()).hasSize(MatchCriterion.values().length);
        assertThat(result.preparation()).isNotEmpty();
    }

    // ── Preparation ─────────────────────────────────

    @Test
    void preparationFollowsTheGapsAndNothingElse() {
        List<String> none = matching.evaluate(readyHome().build(), SpeciesKind.DOG, easyPet().build()).preparation();
        List<String> many = matching.evaluate(
                readyHome().hasCat(true).hasChildrenUnder5(true).activityLevel(1).adjustmentWeeksExpected(1)
                        .housingType(HousingCondition.APARTMENT).monthlyBudget(100_000).build(),
                SpeciesKind.DOG,
                easyPet().energy(5).aloneMinutes(60).noiseLevel(5).fearStrangers(4).build()).preparation();

        assertThat(none).isEmpty();
        assertThat(many).hasSize(8).anySatisfy(step -> assertThat(step).contains("mèo"))
                .anySatisfy(step -> assertThat(step).contains("trẻ nhỏ"))
                .anySatisfy(step -> assertThat(step).contains("vài tuần"));
    }
}
