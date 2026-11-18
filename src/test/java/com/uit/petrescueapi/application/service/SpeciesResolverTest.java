package com.uit.petrescueapi.application.service;

import com.uit.petrescueapi.application.port.out.SpeciesCatalogPort;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpeciesResolverTest {

    private final SpeciesResolver resolver = new SpeciesResolver(new SpeciesCatalogPort() {
        @Override
        public List<SpeciesNames> species() {
            return List.of(new SpeciesNames("Dog", "Chó"), new SpeciesNames("Cat", "Mèo"),
                    new SpeciesNames("Guinea Pig", "Chuột lang"), new SpeciesNames("Other", "Khác"));
        }

        @Override
        public List<SpeciesAlias> aliases() {
            return List.of(new SpeciesAlias("cun", "Dog"), new SpeciesAlias("kitten", "Cat"));
        }
    });

    @Test
    void everySpellingOfDogIsStoredAsDog() {
        assertThat(List.of("Dog", "dog", "DOG", "Chó", "chó", "cho", "cún"))
                .allMatch(s -> "Dog".equals(resolver.canonicalOrOther(s)));
    }

    @Test
    void multiWordNamesMatchToo() {
        assertThat(resolver.resolve("guinea-pig")).contains("Guinea Pig");
        assertThat(resolver.resolve("chuot lang")).contains("Guinea Pig");
    }

    @Test
    void anUnknownSpeciesIsStoredAsOtherAndBlankAsNothing() {
        assertThat(resolver.canonicalOrOther("kỳ nhông")).isEqualTo(SpeciesResolver.OTHER);
        assertThat(resolver.canonicalOrOther("  ")).isNull();
        assertThat(resolver.canonicalOrOther(null)).isNull();
    }

    @Test
    void aFilterOnAnUnknownSpeciesKeepsTheTextSoItMatchesNothing() {
        assertThat(resolver.canonicalForFilter("MÈO")).isEqualTo("Cat");
        assertThat(resolver.canonicalForFilter(" kỳ nhông ")).isEqualTo("kỳ nhông");
        assertThat(resolver.canonicalForFilter("")).isNull();
    }
}
