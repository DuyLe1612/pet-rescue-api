package com.uit.petrescueapi.application.support;

import com.uit.petrescueapi.domain.valueobject.Gender;
import com.uit.petrescueapi.domain.valueobject.HealthStatus;
import com.uit.petrescueapi.domain.valueobject.OrganizationRole;
import com.uit.petrescueapi.domain.valueobject.OrganizationType;
import com.uit.petrescueapi.domain.valueobject.PersonGender;
import com.uit.petrescueapi.domain.valueobject.PetSize;
import com.uit.petrescueapi.domain.valueobject.PetStatus;
import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reads an enum from whatever a person or another client sent: any case, with or without
 * diacritics, with spaces or dashes instead of underscores, or one of the Vietnamese labels the
 * web shows. "male", "Đực" and " MALE " all give {@link Gender#MALE}.
 *
 * <p>The enum constant name is the only value ever stored or returned; aliases are input only.
 * Aliases are written as {@link CanonicalText#key} keys.
 */
public final class EnumAliases {

    private static final Map<Class<?>, Map<String, String>> ALIASES = new HashMap<>();

    static {
        alias(Gender.class, "MALE", "m", "duc", "con_duc", "trong");
        alias(Gender.class, "FEMALE", "f", "cai", "con_cai", "mai");
        alias(Gender.class, "UNKNOWN", "chua_ro", "khong_ro", "chua_ro_gioi_tinh", "unspecified");

        alias(PersonGender.class, "MALE", "m", "nam");
        alias(PersonGender.class, "FEMALE", "f", "nu");
        alias(PersonGender.class, "OTHER", "khac", "unknown", "unspecified", "khong_muon_noi");

        alias(PetSize.class, "SMALL", "s", "nho", "be");
        alias(PetSize.class, "MEDIUM", "m", "vua", "trung_binh", "medium_sized");
        alias(PetSize.class, "LARGE", "l", "lon", "to", "big");

        alias(RescuePriority.class, "CRITICAL", "khan_cap", "rat_gap", "urgent", "emergency");
        alias(RescuePriority.class, "HIGH", "cao", "gap");
        alias(RescuePriority.class, "NORMAL", "binh_thuong", "trung_binh", "medium");
        alias(RescuePriority.class, "LOW", "thap");

        alias(RescueCaseStatus.class, "REPORTED", "moi_bao", "dang_cho_giup", "new", "open");
        alias(RescueCaseStatus.class, "IN_PROGRESS", "dang_cuu", "dang_xu_ly", "dang_duoc_cuu");
        alias(RescueCaseStatus.class, "RESCUED", "da_cuu");
        alias(RescueCaseStatus.class, "CLOSED", "da_dong", "dong");

        alias(PetStatus.class, "AVAILABLE", "san_sang_nhan_nuoi", "cho_nhan_nuoi");
        alias(PetStatus.class, "FOSTERING", "dang_duoc_cham_soc", "nuoi_tam", "fostered");
        alias(PetStatus.class, "ADOPTED", "da_co_gia_dinh", "da_nhan_nuoi", "da_co_chu");
        alias(PetStatus.class, "LOST", "that_lac", "dang_that_lac");
        alias(PetStatus.class, "DECEASED", "da_mat");

        alias(HealthStatus.class, "HEALTHY", "khoe", "khoe_manh");
        alias(HealthStatus.class, "UNDER_TREATMENT", "dang_dieu_tri");
        alias(HealthStatus.class, "RECOVERING", "dang_hoi_phuc");
        alias(HealthStatus.class, "SPECIAL_NEEDS", "can_cham_soc_dac_biet", "cham_soc_dac_biet");

        alias(OrganizationRole.class, "OWNER", "chu", "chu_trung_tam");
        alias(OrganizationRole.class, "STAFF", "nhan_vien");
        alias(OrganizationRole.class, "MEMBER", "thanh_vien");
        alias(OrganizationRole.class, "VET", "bac_si", "bac_si_thu_y", "veterinarian");

        alias(OrganizationType.class, "SHELTER", "tram_cuu_ho", "mai_am", "nha_tam");
        alias(OrganizationType.class, "VET_CENTER", "vet", "clinic", "phong_kham", "phong_kham_thu_y", "co_so_thu_y");
    }

    private EnumAliases() {
    }

    private static void alias(Class<? extends Enum<?>> type, String constant, String... keys) {
        Map<String, String> byKey = ALIASES.computeIfAbsent(type, t -> new HashMap<>());
        for (String key : keys) {
            String previous = byKey.put(key, constant);
            if (previous != null && !previous.equals(constant)) {
                throw new IllegalStateException(type.getSimpleName() + " alias '" + key + "' points at both "
                        + previous + " and " + constant);
            }
        }
    }

    /**
     * The constant {@code raw} stands for, or {@code null} when {@code raw} is null or blank.
     *
     * @throws IllegalArgumentException when nothing matches; the message lists the valid values
     */
    public static <E extends Enum<E>> E parse(Class<E> type, String raw) {
        String key = CanonicalText.key(raw);
        if (key == null) {
            return null;
        }
        for (E constant : type.getEnumConstants()) {
            if (CanonicalText.key(constant.name()).equals(key)) {
                return constant;
            }
        }
        String aliased = ALIASES.getOrDefault(type, Map.of()).get(key);
        if (aliased != null) {
            return Enum.valueOf(type, aliased);
        }
        throw new IllegalArgumentException("'" + raw.trim() + "' is not a valid " + type.getSimpleName()
                + ". Use one of: " + allowed(type));
    }

    /** Like {@link #parse} but gives the constant's name, for fields still stored as text. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static String canonicalName(Class<? extends Enum<?>> type, String raw) {
        Enum<?> constant = parse((Class) type, raw);
        return constant == null ? null : constant.name();
    }

    private static String allowed(Class<? extends Enum<?>> type) {
        return Arrays.stream(type.getEnumConstants()).map(Enum::name).collect(Collectors.joining(", "));
    }
}
