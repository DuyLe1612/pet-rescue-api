package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.Notification;
import com.uit.petrescueapi.domain.repository.NotificationRepository;
import com.uit.petrescueapi.domain.valueobject.NotificationType;
import com.uit.petrescueapi.infrastructure.persistence.entity.NotificationJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.repository.NotificationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final NotificationJpaRepository jpa;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Notification> saveAll(Collection<Notification> notifications) {
        return jpa.saveAll(notifications.stream().map(NotificationRepositoryAdapter::toEntity).toList()).stream()
                .map(NotificationRepositoryAdapter::toDomain).toList();
    }

    @Override
    public Page<Notification> findByUser(UUID userId, boolean unreadOnly, Pageable pageable) {
        Page<NotificationJpaEntity> page = unreadOnly
                ? jpa.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId, pageable)
                : jpa.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return page.map(NotificationRepositoryAdapter::toDomain);
    }

    @Override
    public long countUnread(UUID userId) {
        return jpa.countByUserIdAndReadAtIsNull(userId);
    }

    @Override
    public boolean markRead(UUID notificationId, UUID userId) {
        return jpa.markRead(notificationId, userId, LocalDateTime.now()) > 0;
    }

    @Override
    public boolean existsForUser(UUID notificationId, UUID userId) {
        return jpa.existsByNotificationIdAndUserId(notificationId, userId);
    }

    @Override
    public int markAllRead(UUID userId) {
        return jpa.markAllRead(userId, LocalDateTime.now());
    }

    @Override
    public List<UUID> findAdminUserIds() {
        return jdbcTemplate.queryForList("""
                SELECT ur.user_id
                FROM user_roles ur
                JOIN roles r ON r.role_id = ur.role_id
                JOIN users u ON u.user_id = ur.user_id
                WHERE r.code = 'ADMIN' AND u.is_deleted = false
                """, UUID.class);
    }

    @Override
    public List<UUID> findOrganizationHandlerIds(UUID organizationId) {
        return jdbcTemplate.queryForList("""
                SELECT om.user_id
                FROM organization_members om
                WHERE om.organization_id = ?
                  AND om.status = 'ACTIVE'
                  AND om.role IN ('OWNER', 'STAFF')
                """, UUID.class, organizationId);
    }

    private static Notification toDomain(NotificationJpaEntity e) {
        return Notification.builder()
                .notificationId(e.getNotificationId())
                .userId(e.getUserId())
                .type(NotificationType.valueOf(e.getType()))
                .title(e.getTitle())
                .body(e.getBody())
                .data(e.getData())
                .readAt(e.getReadAt())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private static NotificationJpaEntity toEntity(Notification n) {
        return NotificationJpaEntity.builder()
                .notificationId(n.getNotificationId())
                .userId(n.getUserId())
                .type(n.getType().name())
                .title(n.getTitle())
                .body(n.getBody())
                .data(n.getData())
                .readAt(n.getReadAt())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
