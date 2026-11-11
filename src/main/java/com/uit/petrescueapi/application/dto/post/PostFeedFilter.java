package com.uit.petrescueapi.application.dto.post;

/**
 * Optional post filters. {@code category}, {@code species}, {@code district}, {@code province} and
 * {@code needsRescue} match the user-confirmed post facets (DA2 Intelligent Post Processing), never the
 * raw AI suggestion; {@code tag} matches a tag code. Null fields are ignored.
 */
public record PostFeedFilter(String category, String species, String district, String province,
                             Boolean needsRescue, String tag) {

    public PostFeedFilter(String category, String species, String district, String tag) {
        this(category, species, district, null, null, tag);
    }

    public PostFeedFilter {
        category = blankToNull(category);
        species = blankToNull(species);
        district = blankToNull(district);
        province = blankToNull(province);
        tag = blankToNull(tag);
    }

    public static PostFeedFilter none() {
        return new PostFeedFilter(null, null, null, null, null, null);
    }

    public boolean isEmpty() {
        return category == null && species == null && district == null && province == null
                && needsRescue == null && tag == null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
