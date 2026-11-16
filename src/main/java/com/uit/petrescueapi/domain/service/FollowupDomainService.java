package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.repository.FollowupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Follow-up after an adoption. The answers never touch reputation: if honesty were punished,
 * adopters would stop being honest.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FollowupDomainService {

    private final FollowupRepository repository;

    /** Offsets from completion to each milestone: day 2, week 2, month 3, month 6. */
    @Value("${app.followup.offsets:P2D,P14D,P90D,P180D}")
    private List<Duration> offsets;

    public void scheduleFor(UUID applicationId, LocalDateTime completedAt) {
        repository.schedule(applicationId, completedAt, offsets);
    }

    public void cancelOpen(UUID applicationId) {
        repository.cancelOpen(applicationId);
    }
}
