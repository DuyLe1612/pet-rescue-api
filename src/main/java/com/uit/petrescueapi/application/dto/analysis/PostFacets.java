package com.uit.petrescueapi.application.dto.analysis;

import com.uit.petrescueapi.domain.valueobject.PostCategory;

/** Structured post data confirmed by a human — the only values the feed filters on. */
public record PostFacets(PostCategory category, String species, String district, String province, boolean needsRescue) {
}
