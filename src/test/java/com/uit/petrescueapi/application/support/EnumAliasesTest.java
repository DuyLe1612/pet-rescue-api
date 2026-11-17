package com.uit.petrescueapi.application.support;

import com.uit.petrescueapi.domain.valueobject.Gender;
import com.uit.petrescueapi.domain.valueobject.OrganizationType;
import com.uit.petrescueapi.domain.valueobject.PersonGender;
import com.uit.petrescueapi.domain.valueobject.PetSize;
import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnumAliasesTest {

    @Test
    void anyCaseOrSpacingOfTheConstantNameIsAccepted() {
        assertThat(List.of("MALE", "male", " Male ")).allMatch(s -> EnumAliases.parse(Gender.class, s) == Gender.MALE);
        assertThat(EnumAliases.parse(RescueCaseStatus.class, "in progress")).isEqualTo(RescueCaseStatus.IN_PROGRESS);
        assertThat(EnumAliases.parse(RescueCaseStatus.class, "in-progress")).isEqualTo(RescueCaseStatus.IN_PROGRESS);
    }

    @Test
    void theVietnameseLabelsTheWebShowsAreAccepted() {
        assertThat(EnumAliases.parse(Gender.class, "Đực")).isEqualTo(Gender.MALE);
        assertThat(EnumAliases.parse(Gender.class, "cái")).isEqualTo(Gender.FEMALE);
        assertThat(EnumAliases.parse(PetSize.class, "Nhỏ")).isEqualTo(PetSize.SMALL);
        assertThat(EnumAliases.parse(RescuePriority.class, "Khẩn cấp")).isEqualTo(RescuePriority.CRITICAL);
        assertThat(EnumAliases.parse(OrganizationType.class, "Phòng khám thú y")).isEqualTo(OrganizationType.VET_CENTER);
        assertThat(EnumAliases.parse(PersonGender.class, "Nữ")).isEqualTo(PersonGender.FEMALE);
    }

    @Test
    void anAliasOnlyCountsForItsOwnEnum() {
        // "nam" is a man, not a male pet
        assertThatThrownBy(() -> EnumAliases.parse(Gender.class, "nam")).isInstanceOf(IllegalArgumentException.class);
        assertThat(EnumAliases.parse(PersonGender.class, "nam")).isEqualTo(PersonGender.MALE);
    }

    @Test
    void unknownTextIsRefusedWithTheValidValues() {
        assertThatThrownBy(() -> EnumAliases.parse(PetSize.class, "huge"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'huge'")
                .hasMessageContaining("SMALL, MEDIUM, LARGE");
    }

    @Test
    void blankMeansNotGiven() {
        assertThat(EnumAliases.parse(Gender.class, null)).isNull();
        assertThat(EnumAliases.parse(Gender.class, " ")).isNull();
        assertThat(EnumAliases.canonicalName(Gender.class, "")).isNull();
    }

    @Test
    void textFieldsGetTheConstantName() {
        assertThat(EnumAliases.canonicalName(PersonGender.class, "khác")).isEqualTo("OTHER");
    }
}
