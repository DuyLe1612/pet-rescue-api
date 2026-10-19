package com.uit.petrescueapi.application.dto.post;

/**
 * Optional feed filters. {@code category}, {@code species} and {@code district} match the
 * user-confirmed post facets (DA2 Intelligent Post Processing); {@code tag} matches a tag code.
 * Null fields are ignored. Only applied when posts are stored in MongoDB.
 */
public record PostFeedFilter(String category, String species, String district, String tag) {

    public static PostFeedFilter none() {
        return new PostFeedFilter(null, null, null, null);
    }

    public boolean isEmpty() {
        return category == null && species == null && district == null && tag == null;
    }
}
