package com.uit.petrescueapi.application.dto.post;

import java.util.UUID;

/**
 * Optional post filters. {@code category}, {@code species}, {@code district}, {@code province} and
 * {@code needsRescue} match the user-confirmed post facets (DA2 Intelligent Post Processing), never the
 * raw AI suggestion; {@code tag} matches a tag code; {@code rescueCaseId} and {@code hasOpenCase}
 * match the rescue case a post is linked to. Null fields are ignored.
 */
public record PostFeedFilter(String category, String species, String district, String province,
                             Boolean needsRescue, String tag, UUID rescueCaseId, Boolean hasOpenCase) {

    public PostFeedFilter(String category, String species, String district, String tag) {
        this(category, species, district, null, null, tag, null, null);
    }

    public PostFeedFilter(String category, String species, String district, String province, Boolean needsRescue, String tag) {
        this(category, species, district, province, needsRescue, tag, null, null);
    }

    public PostFeedFilter {
        category = blankToNull(category);
        species = blankToNull(species);
        district = blankToNull(district);
        province = blankToNull(province);
        tag = blankToNull(tag);
    }

    /** The same filter with another province, e.g. the catalogue name for "sg" or "79". */
    public PostFeedFilter withProvince(String value) {
        return new PostFeedFilter(category, species, district, value, needsRescue, tag, rescueCaseId, hasOpenCase);
    }

    public static PostFeedFilter none() {
        return new PostFeedFilter(null, null, null, null, null, null, null, null);
    }

    public boolean isEmpty() {
        return category == null && species == null && district == null && province == null
                && needsRescue == null && tag == null && rescueCaseId == null && hasOpenCase == null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
