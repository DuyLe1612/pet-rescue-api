package com.uit.petrescueapi.application.service;

import com.uit.petrescueapi.domain.entity.RescueCase;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TriageRulesTest {

    private static RescueCase.RescueCaseBuilder<?, ?> complete() {
        return RescueCase.builder().species("Dog").color("Vàng").size("Vừa")
                .description("Bị xe đụng, chảy máu ở chân sau").locationText("Cầu Thị Nghè")
                .wardName("Phường 19").provinceName("Hồ Chí Minh");
    }

    @Test
    void aCompleteReportLacksNothing() {
        assertThat(TriageRules.missingFields(complete().build(), 2)).isEmpty();
    }

    @Test
    void eachGapIsNamedInAStableOrder() {
        assertThat(TriageRules.missingFields(complete().build(), 0)).containsExactly(TriageRules.PHOTO);
        assertThat(TriageRules.missingFields(complete().description("  ").build(), 1)).containsExactly(TriageRules.DESCRIPTION);
        assertThat(TriageRules.missingFields(complete().description("chó bị thương").build(), 1))
                .as("thirteen characters say enough to count").isEmpty();
        assertThat(TriageRules.missingFields(complete().description("bị thương").build(), 1))
                .as("under ten characters says almost nothing").containsExactly(TriageRules.DESCRIPTION);
        assertThat(TriageRules.missingFields(complete().species(null).build(), 1)).containsExactly(TriageRules.SPECIES);
        assertThat(TriageRules.missingFields(complete().locationText(null).wardName(null).build(), 1)).containsExactly(TriageRules.LOCATION);
        assertThat(TriageRules.missingFields(
                RescueCase.builder().build(), 0))
                .containsExactly(TriageRules.PHOTO, TriageRules.DESCRIPTION, TriageRules.SPECIES, TriageRules.LOCATION);
    }

    @Test
    void aWardAloneIsEnoughToFindTheAnimal() {
        assertThat(TriageRules.missingFields(complete().locationText(null).build(), 1)).isEmpty();
    }

    @Test
    void theModelReadsLabelledLinesWithoutBlanksAndWithoutTheReportersPriority() {
        String text = TriageRules.textFor(complete().priority(RescuePriority.LOW).build());

        assertThat(text).isEqualTo("""
                Loài: Dog
                Màu: Vàng
                Kích cỡ: Vừa
                Địa điểm: Cầu Thị Nghè, Phường 19, Hồ Chí Minh
                Mô tả: Bị xe đụng, chảy máu ở chân sau""");
        assertThat(text).doesNotContain("LOW").doesNotContain("priority");
    }

    @Test
    void emptyFieldsAndRepeatedPlacesAreLeftOut() {
        String text = TriageRules.textFor(RescueCase.builder().species("Cat").description("  mèo con  ")
                .locationText("Quận 7").wardName("Quận 7").provinceName(" ").build());

        assertThat(text).isEqualTo("Loài: Cat\nĐịa điểm: Quận 7\nMô tả: mèo con");
    }
}
