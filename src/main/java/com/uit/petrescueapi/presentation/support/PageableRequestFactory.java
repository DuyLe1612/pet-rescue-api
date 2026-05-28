package com.uit.petrescueapi.presentation.support;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class PageableRequestFactory {

    private PageableRequestFactory() {
    }

    public static Pageable of(int page, int pageSize, String sortBy, String sortOrder) {
        String resolvedSortBy = normalizeSortField((sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy.trim());
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, pageSize, Sort.by(direction, resolvedSortBy));
    }

    private static String normalizeSortField(String sortField) {
        StringBuilder builder = new StringBuilder(sortField.length() + 8);
        for (int index = 0; index < sortField.length(); index++) {
            char character = sortField.charAt(index);
            if (Character.isUpperCase(character)) {
                builder.append('_').append(Character.toLowerCase(character));
            } else {
                builder.append(character);
            }
        }
        return builder.toString();
    }
}