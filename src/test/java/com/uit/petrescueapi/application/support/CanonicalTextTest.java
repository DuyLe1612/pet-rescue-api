package com.uit.petrescueapi.application.support;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CanonicalTextTest {

    @Test
    void everySpellingOfOneWordGivesOneKey() {
        assertThat(List.of("Dog", "DOG", " dog ", "dog")).allMatch(s -> "dog".equals(CanonicalText.key(s)));
        assertThat(List.of("Chó", "chó", "CHÓ", "cho")).allMatch(s -> "cho".equals(CanonicalText.key(s)));
    }

    @Test
    void vietnameseLettersLoseTheirMarksIncludingDBar() {
        assertThat(CanonicalText.key("Đực")).isEqualTo("duc");
        assertThat(CanonicalText.key("Chuột lang")).isEqualTo("chuot_lang");
        assertThat(CanonicalText.key("Khẩn cấp")).isEqualTo("khan_cap");
    }

    @Test
    void decomposedAccentsGiveTheSameKeyAsComposedOnes() {
        String decomposed = java.text.Normalizer.normalize("Mèo", java.text.Normalizer.Form.NFD);
        assertThat(CanonicalText.key(decomposed)).isEqualTo(CanonicalText.key("Mèo")).isEqualTo("meo");
    }

    @Test
    void separatorsCollapseAndNeverSitAtTheEnds() {
        assertThat(CanonicalText.key("  in_progress ")).isEqualTo("in_progress");
        assertThat(CanonicalText.key("In Progress")).isEqualTo("in_progress");
        assertThat(CanonicalText.key("--Shih-Tzu!!")).isEqualTo("shih_tzu");
    }

    @Test
    void blankOrSymbolsOnlyIsNoKey() {
        assertThat(CanonicalText.key(null)).isNull();
        assertThat(CanonicalText.key("   ")).isNull();
        assertThat(CanonicalText.key("!!!")).isNull();
    }

    @Test
    void tagCodesAreHyphenSlugs() {
        assertThat(CanonicalText.slug("Tìm chủ")).isEqualTo("tim-chu");
        assertThat(CanonicalText.slug("LOST_PET")).isEqualTo("lost-pet");
        assertThat(CanonicalText.slug("lost-pet")).isEqualTo("lost-pet");
    }

    @Test
    void tagListsAreSluggedDeduplicatedAndKeepTheirOrder() {
        assertThat(CanonicalText.slugs(Arrays.asList("Rescue", "rescue", " ", null, "Lost Pet", "lost-pet")))
                .containsExactly("rescue", "lost-pet");
        assertThat(CanonicalText.slugs(null)).isNull();
    }
}
