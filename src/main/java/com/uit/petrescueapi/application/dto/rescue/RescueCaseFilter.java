package com.uit.petrescueapi.application.dto.rescue;

import com.uit.petrescueapi.domain.valueobject.RescueCaseStatus;
import com.uit.petrescueapi.domain.valueobject.RescuePriority;

import java.util.List;

/** What every rescue case list can be narrowed by. A null or empty value does not filter. */
public record RescueCaseFilter(
        String search,
        List<RescueCaseStatus> statuses,
        List<RescuePriority> priorities,
        String species) {
}
