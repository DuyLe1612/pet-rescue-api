package com.uit.petrescueapi.presentation.support;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.uit.petrescueapi.domain.exception.BusinessException;

import java.util.Map;
import java.util.TreeSet;

public final class PageableRequestFactory {

    private PageableRequestFactory() {
    }

    public static Pageable of(int page, int pageSize, String sortBy, String sortOrder) {
        String resolvedSortBy = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy.trim();
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, pageSize, Sort.by(direction, resolvedSortBy));
    }

    public static Pageable ofNative(int page, int pageSize, String sortBy, String sortOrder) {
        String resolvedSortBy = normalizeSortField((sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy.trim());
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, pageSize, Sort.by(direction, resolvedSortBy));
    }

    /**
     * For native queries, where the sort is appended to the SQL: only the listed fields are accepted.
     *
     * @param columns accepted {@code sortBy} value → column or select alias to sort on
     */
    public static Pageable ofNative(int page, int pageSize, String sortBy, String sortOrder, Map<String, String> columns) {
        String requested = sortBy == null ? "" : sortBy.trim();
        String column = columns.get(requested);
        if (column == null) {
            throw new BusinessException(
                    "Cannot sort by '" + requested + "'. Allowed: " + String.join(", ", new TreeSet<>(columns.keySet())),
                    "INVALID_SORT");
        }
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(page, pageSize, Sort.by(direction, column));
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