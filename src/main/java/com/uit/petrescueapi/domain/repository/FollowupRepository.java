package com.uit.petrescueapi.domain.repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Storage of follow-up schedules, answers, alerts and outcomes. */
public interface FollowupRepository {

    /** Records the completion time and creates one row per milestone; repeating it adds nothing. */
    void schedule(UUID applicationId, LocalDateTime completedAt, List<Duration> offsets);

    /** Cancels every milestone that has not been answered. Returns how many were cancelled. */
    int cancelOpen(UUID applicationId);
}
