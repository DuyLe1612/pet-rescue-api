package com.uit.petrescueapi.application.service;

import com.uit.petrescueapi.application.dto.analysis.PostAnalysisSuggestion;
import com.uit.petrescueapi.application.port.out.PostAnalysisPort.Catalog;
import com.uit.petrescueapi.domain.valueobject.PostCategory;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostAnalysisValidatorTest {

    final Catalog catalog = new Catalog(List.of("Dog", "Cat"), List.of("injured", "dog", "q7"));
    final PostAnalysisValidator validator = com.uit.petrescueapi.support.TestPlaces.validator();

    private PostAnalysisSuggestion suggestion(PostCategory category, String species, boolean needsRescue,
                                              RescuePriority urgency, List<String> tags, Double confidence) {
        return new PostAnalysisSuggestion(category, species, "Q7", "  Quận 7 ", " ", needsRescue, "Possible leg injury",
                urgency, tags, confidence, confidence, confidence);
    }

    @Test
    void validSuggestionPassesAndIsNormalised() {
        var out = validator.validate(suggestion(PostCategory.RESCUE, "dog", true, RescuePriority.HIGH,
                List.of(" Injured", "dog", "dog", "made-up"), 0.9), catalog);

        assertThat(out.errors()).isEmpty();
        assertThat(out.fatal()).isFalse();
        assertThat(out.suggestion().species()).isEqualTo("Dog");          // canonical taxonomy name
        assertThat(out.suggestion().district()).isEqualTo("Quận 7 (cũ)"); // districts no longer exist
        assertThat(out.suggestion().province()).isEqualTo("Thành phố Hồ Chí Minh"); // a numbered district is HCMC
        assertThat(out.suggestion().tags()).containsExactly("injured", "dog"); // unknown tags dropped silently
        assertThat(out.suggestion().urgency()).isEqualTo(RescuePriority.HIGH);
    }

    @Test
    void theProvinceIsStoredAsTheCatalogueNamesIt() {
        var out = validator.validate(new PostAnalysisSuggestion(PostCategory.LOST, "Cat", "P. Long Bình, SG", "P. Long Bình",
                "sg", false, null, null, List.of(), 0.9, 0.9, 0.8), catalog);

        assertThat(out.errors()).isEmpty();
        assertThat(out.suggestion().province()).isEqualTo("Thành phố Hồ Chí Minh");
        assertThat(out.suggestion().district()).isEqualTo("Phường Long Bình");
        assertThat(out.suggestion().locationText()).isEqualTo("P. Long Bình, SG"); // what the author wrote is kept
    }

    @Test
    void anUnknownProvinceIsClearedAndReported() {
        var out = validator.validate(new PostAnalysisSuggestion(PostCategory.LOST, "Cat", null, null,
                "Atlantis", false, null, null, List.of(), 0.9, 0.9, 0.8), catalog);

        assertThat(out.suggestion().province()).isNull();
        assertThat(out.suggestion().locationConfidence()).isNull();
        assertThat(out.errors()).anyMatch(e -> e.contains("Atlantis"));
    }

    @Test
    void missingCategoryIsFatal() {
        var out = validator.validate(suggestion(null, "Dog", false, null, List.of(), 0.5), catalog);
        assertThat(out.fatal()).isTrue();
        assertThat(out.errors()).anyMatch(e -> e.contains("category"));
    }

    @Test
    void unknownSpeciesIsClearedAndReported() {
        var out = validator.validate(suggestion(PostCategory.FOUND, "Dragon", false, null, List.of(), 0.7), catalog);
        assertThat(out.fatal()).isFalse();
        assertThat(out.suggestion().species()).isNull();
        assertThat(out.suggestion().speciesConfidence()).isNull();
        assertThat(out.errors()).anyMatch(e -> e.contains("Dragon"));
    }

    @Test
    void confidenceOutsideZeroToOneIsClampedAndReported() {
        var out = validator.validate(suggestion(PostCategory.GENERAL, null, false, null, List.of(), 1.7), catalog);
        assertThat(out.suggestion().categoryConfidence()).isEqualTo(1.0);
        assertThat(out.errors()).anyMatch(e -> e.contains("confidence"));
    }

    @Test
    void urgencyIsDroppedWhenNoRescueIsNeeded() {
        var out = validator.validate(suggestion(PostCategory.ADOPTION, "Cat", false, RescuePriority.CRITICAL, List.of(), 0.8), catalog);
        assertThat(out.suggestion().urgency()).isNull();
        assertThat(out.errors()).isEmpty();
    }

    @Test
    void atMostTenTagsAreKept() {
        Catalog many = new Catalog(List.of(), java.util.stream.IntStream.range(0, 15).mapToObj(i -> "t" + i).toList());
        var out = validator.validate(suggestion(PostCategory.GENERAL, null, false, null, many.tagCodes(), 0.5), many);
        assertThat(out.suggestion().tags()).hasSize(10);
    }
}
