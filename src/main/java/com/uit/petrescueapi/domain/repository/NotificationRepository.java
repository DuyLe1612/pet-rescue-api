package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Domain repository contract for in-app notifications and their recipient lookups. */
public interface NotificationRepository {

    List<Notification> saveAll(Collection<Notification> notifications);

    Page<Notification> findByUser(UUID userId, boolean unreadOnly, Pageable pageable);

    long countUnread(UUID userId);

    boolean markRead(UUID notificationId, UUID userId);

    boolean existsForUser(UUID notificationId, UUID userId);

    int markAllRead(UUID userId);

    List<UUID> findAdminUserIds();

    /** Active OWNER and STAFF members of the organization. */
    List<UUID> findOrganizationHandlerIds(UUID organizationId);
}
