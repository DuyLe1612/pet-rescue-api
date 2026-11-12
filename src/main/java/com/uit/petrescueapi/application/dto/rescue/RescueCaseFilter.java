package com.uit.petrescueapi.application.dto.rescue;

import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;

import java.util.List;
import java.util.UUID;

/**
 * Optional filters shared by the rescue case list, nearby and bounding-box queries.
 * A null or empty value means "do not filter on this".
 *
 * @param species    matched case-insensitively against the reporter's free text
 * @param province   a province code, or a province name (case-insensitive)
 * @param reporterId only cases this user reported
 */
public record RescueCaseFilter(List<RescueCaseStatus> statuses, List<RescuePriority> priorities,
                               String species, String province, UUID reporterId) {

    public RescueCaseFilter(List<RescueCaseStatus> statuses, List<RescuePriority> priorities, String species, String province) {
        this(statuses, priorities, species, province, null);
    }

    public static RescueCaseFilter none() {
        return new RescueCaseFilter(null, null, null, null, null);
    }

    public static RescueCaseFilter reportedBy(UUID reporterId, List<RescueCaseStatus> statuses) {
        return new RescueCaseFilter(statuses, null, null, null, reporterId);
    }

    public RescueCaseFilter {
        species = blankToNull(species);
        province = blankToNull(province);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
