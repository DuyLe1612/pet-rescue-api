package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.NotificationJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface NotificationJpaRepository extends JpaRepository<NotificationJpaEntity, UUID> {

    Page<NotificationJpaEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<NotificationJpaEntity> findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(UUID userId);

    boolean existsByNotificationIdAndUserId(UUID notificationId, UUID userId);

    @Modifying
    @Query("""
            UPDATE NotificationJpaEntity n SET n.readAt = :now
            WHERE n.notificationId = :id AND n.userId = :userId AND n.readAt IS NULL
            """)
    int markRead(@Param("id") UUID id, @Param("userId") UUID userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE NotificationJpaEntity n SET n.readAt = :now WHERE n.userId = :userId AND n.readAt IS NULL")
    int markAllRead(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
}
