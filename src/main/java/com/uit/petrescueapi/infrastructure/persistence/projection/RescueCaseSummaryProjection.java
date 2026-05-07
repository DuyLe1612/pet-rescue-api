package com.uit.petrescueapi.infrastructure.persistence.projection;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Spring Data interface projection for rescue case summary list queries.
 *
 * <p>Maps from JPQL {@code SELECT ... FROM rescue_cases rc LEFT JOIN users u}
 * query. Column aliases must match getter names (camelCase).</p>
 */
public interface RescueCaseSummaryProjection {

    UUID getCaseId();
    String getCaseCode();
    String getSpecies();
    String getPriority();
    String getStatus();
    LocalDateTime getReportedAt();
    String getLocationText();
    String getReporterUsername();
    String getFirstImageUrl();
}
