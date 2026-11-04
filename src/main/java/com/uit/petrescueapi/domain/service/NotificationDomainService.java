package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.Notification;
import com.uit.petrescueapi.domain.event.NotificationsCreatedEvent;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.repository.NotificationRepository;
import com.uit.petrescueapi.domain.valueobject.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Stores notifications in the caller's transaction. Delivery happens after commit, so a rolled-back
 * action never reaches anyone.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationDomainService {

    private final NotificationRepository repository;
    private final ApplicationEventPublisher events;

    public void notifyUsers(Collection<UUID> userIds, NotificationType type, String title, String body,
                            Map<String, String> data) {
        LocalDateTime now = LocalDateTime.now();
        List<Notification> notifications = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .map(userId -> Notification.builder()
                        .notificationId(UUID.randomUUID())
                        .userId(userId)
                        .type(type)
                        .title(title)
                        .body(body)
                        .data(data)
                        .createdAt(now)
                        .build())
                .toList();
        if (notifications.isEmpty()) {
            return;
        }
        events.publishEvent(new NotificationsCreatedEvent(repository.saveAll(notifications)));
    }

    public void notifyAdmins(NotificationType type, String title, String body, Map<String, String> data) {
        notifyUsers(repository.findAdminUserIds(), type, title, body, data);
    }

    public void notifyOrganizationHandlers(UUID organizationId, NotificationType type, String title, String body,
                                           Map<String, String> data) {
        notifyUsers(repository.findOrganizationHandlerIds(organizationId), type, title, body, data);
    }

    @Transactional(readOnly = true)
    public Page<Notification> findForUser(UUID userId, boolean unreadOnly, Pageable pageable) {
        return repository.findByUser(userId, unreadOnly, pageable);
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return repository.countUnread(userId);
    }

    /** Someone else's notification looks the same as a missing one. Marking twice is harmless. */
    public void markRead(UUID notificationId, UUID userId) {
        if (!repository.markRead(notificationId, userId) && !repository.existsForUser(notificationId, userId)) {
            throw new ResourceNotFoundException("Notification", "notificationId", notificationId);
        }
    }

    public int markAllRead(UUID userId) {
        return repository.markAllRead(userId);
    }
}
