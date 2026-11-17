package com.uit.petrescueapi.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.uit.petrescueapi.application.dto.auth.RegisterRequestDto;
import com.uit.petrescueapi.application.dto.pet.CreatePetRequestDto;
import com.uit.petrescueapi.application.dto.rescue.CreateRescueCaseRequestDto;
import com.uit.petrescueapi.domain.valueobject.Gender;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Request bodies go through the application's own ObjectMapper. */
class CanonicalEnumModuleTest {

    private final ObjectMapper mapper = new JacksonConfig().objectMapper();

    @Test
    void enumFieldsAcceptAnySpellingOfAKnownValue() throws Exception {
        CreatePetRequestDto pet = mapper.readValue("{\"gender\":\"đực\"}", CreatePetRequestDto.class);
        assertThat(pet.getGender()).isEqualTo(Gender.MALE);

        CreateRescueCaseRequestDto rescue = mapper.readValue("{\"priority\":\"khẩn cấp\"}", CreateRescueCaseRequestDto.class);
        assertThat(rescue.getPriority()).isEqualTo(RescuePriority.CRITICAL);
    }

    @Test
    void annotatedTextFieldsHoldTheConstantName() throws Exception {
        CreateRescueCaseRequestDto rescue = mapper.readValue("{\"size\":\"Lớn\"}", CreateRescueCaseRequestDto.class);
        assertThat(rescue.getSize()).isEqualTo("LARGE");

        RegisterRequestDto register = mapper.readValue("{\"gender\":\"Nữ\"}", RegisterRequestDto.class);
        assertThat(register.getGender()).isEqualTo("FEMALE");
    }

    @Test
    void absentOrNullStaysNull() throws Exception {
        assertThat(mapper.readValue("{}", CreateRescueCaseRequestDto.class).getSize()).isNull();
        assertThat(mapper.readValue("{\"size\":null}", CreateRescueCaseRequestDto.class).getSize()).isNull();
        assertThat(mapper.readValue("{\"gender\":null}", CreatePetRequestDto.class).getGender()).isNull();
    }

    @Test
    void unknownValuesAreRefusedWithTheValidOnes() {
        assertThatThrownBy(() -> mapper.readValue("{\"size\":\"huge\"}", CreateRescueCaseRequestDto.class))
                .isInstanceOf(InvalidFormatException.class)
                .hasMessageContaining("SMALL, MEDIUM, LARGE");
        assertThatThrownBy(() -> mapper.readValue("{\"gender\":\"robot\"}", CreatePetRequestDto.class))
                .isInstanceOf(InvalidFormatException.class)
                .hasMessageContaining("MALE, FEMALE, UNKNOWN");
    }
}
