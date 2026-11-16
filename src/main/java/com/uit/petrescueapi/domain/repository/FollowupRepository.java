package com.uit.petrescueapi.domain.repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Storage of follow-up schedules, answers, alerts and outcomes. */
public interface FollowupRepository {

    /** One milestone with the pet's name; {@code answers} is only filled when the adopter responded. */
    record Row(UUID followupId, UUID applicationId, UUID petId, UUID adopterId, UUID organizationId, String milestone,
               String status, String petName, LocalDateTime dueAt, LocalDateTime sentAt, LocalDateTime respondedAt,
               LocalDateTime expiresAt, String severity, Boolean stillHasPet, Map<String, Object> answers) {
    }

    record Contact(String email, String name) {
    }

    /** Records the completion time and creates one row per milestone; repeating it adds nothing. */
    void schedule(UUID applicationId, LocalDateTime completedAt, List<Duration> offsets);

    /** Cancels every milestone that has not been answered. Returns how many were cancelled. */
    int cancelOpen(UUID applicationId);

    /** Milestones that are due and have not been sent. */
    List<UUID> findDueIds(LocalDateTime now, int limit);

    /** An older milestone that is still unsent when a later one is also due is dropped: ask the newest question only. */
    int expireSuperseded(LocalDateTime now);

    /** Marks one due milestone as sent and stores the hash of its link token; empty if someone else got it first. */
    Optional<Row> claim(UUID followupId, String tokenHash, LocalDateTime now, LocalDateTime expiresAt);

    Optional<Row> findByTokenHash(String tokenHash);

    Optional<Row> findById(UUID followupId);

    List<Row> findByApplication(UUID applicationId);

    /** Stores the answers only while the milestone is still open (sent and not expired); false otherwise. */
    boolean recordAnswers(UUID followupId, Map<String, Object> answers, Boolean stillHasPet, String severity, LocalDateTime now);

    /** Marks sent-but-unanswered milestones as reminded and returns them. */
    List<Row> claimReminders(LocalDateTime now, LocalDateTime sentBefore);

    /** Marks sent milestones past their expiry as expired and returns them. */
    List<Row> expireOverdue(LocalDateTime now);

    Optional<Contact> findContact(UUID userId);

    /** An active owner or staff member of the organization. */
    boolean isOrganizationHandler(UUID organizationId, UUID userId);
}
