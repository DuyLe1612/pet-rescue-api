package com.uit.petrescueapi.application.service;

import com.uit.petrescueapi.support.TestPlaces;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Against the real 34-province catalogue that ships with the app. */
class VietnamPlaceResolverTest {

    static final String HCMC = "Thành phố Hồ Chí Minh";
    static final String HANOI = "Thành phố Hà Nội";

    final VietnamPlaceResolver resolver = TestPlaces.resolver();

    @ParameterizedTest(name = "province \"{0}\"")
    @CsvSource({
            "TP. Hồ Chí Minh", "tp hcm", "TPHCM", "HCM", "sg", "Sài Gòn", "sai gon", "Thành phố Hồ Chí Minh",
            "Bình Dương", "Vũng Tàu"})
    void everyWayOfWritingHoChiMinhCityResolvesToTheCatalogueName(String written) {
        var place = resolver.resolve(written, null, null);

        assertThat(place.provinceName()).isEqualTo(HCMC);
        assertThat(place.provinceCode()).isEqualTo(79);
    }

    @ParameterizedTest(name = "area \"{0}\"")
    @CsvSource({"q9, Quận 9 (cũ)", "Q.9, Quận 9 (cũ)", "Q 9, Quận 9 (cũ)", "quan 9, Quận 9 (cũ)", "Quận 09, Quận 9 (cũ)",
            "Q7, Quận 7 (cũ)"})
    void aNumberedDistrictMeansHoChiMinhCityAndStaysAnOldLabel(String written, String label) {
        var place = resolver.resolve(null, written, null);

        assertThat(place.provinceName()).isEqualTo(HCMC);
        assertThat(place.wardName()).isNull();              // districts are gone; never invent a ward
        assertThat(place.area()).isEqualTo(label);
    }

    @Test
    void aNumberedDistrictInsideTheTextIsEnoughForTheProvince() {
        var place = resolver.resolve(null, null, "bé cún bị thương ở hẻm gần chợ q9 nha mn");

        assertThat(place.provinceName()).isEqualTo(HCMC);
    }

    @Test
    void namedOldDistrictsTellTheProvince() {
        assertThat(resolver.resolve(null, "Gò Vấp", null).provinceName()).isEqualTo(HCMC);
        assertThat(resolver.resolve(null, "quận cầu giấy", null).provinceName()).isEqualTo(HANOI);
        assertThat(resolver.resolve(null, "Cau Giay", null).area()).isEqualTo("Cầu Giấy (cũ)");
    }

    @Test
    void anExplicitWardIsMatchedToTheCatalogue() {
        var place = resolver.resolve("HCM", "P. Long Bình", null);

        assertThat(place.wardName()).isEqualTo("Phường Long Bình");
        assertThat(place.wardCode()).isEqualTo(26833);
        assertThat(place.area()).isEqualTo("Phường Long Bình");
    }

    @Test
    void aBareNameIsNotTakenForAWard() {
        // "Bình Thạnh" is both an old district and a new ward; without "phường" we do not choose
        var place = resolver.resolve(null, "Bình Thạnh", null);

        assertThat(place.provinceName()).isEqualTo(HCMC);
        assertThat(place.wardName()).isNull();
        assertThat(place.area()).isEqualTo("Bình Thạnh (cũ)");
    }

    @Test
    void twoProvincesInOneTextAreNotGuessedBetween() {
        var place = resolver.resolve(null, null, "chuyển bé từ Hà Nội vào Hồ Chí Minh");

        assertThat(place.provinceName()).isNull();
    }

    @Test
    void nothingKnownStaysUnknown() {
        assertThat(resolver.resolve(null, null, "hẻm nhà mình")).isEqualTo(VietnamPlaceResolver.Place.NONE);
        assertThat(resolver.resolve("Atlantis", null, null).provinceName()).isNull();
    }
}
