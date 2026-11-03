package com.uit.petrescueapi.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationJpaEntity {

    @Id
    @Column(name = "notification_id", updatable = false, nullable = false)
    private UUID notificationId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    private String body;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data")
    private Map<String, String> data;

    @Column(name = "read_at", columnDefinition = "TIMESTAMPTZ")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ")
    private LocalDateTime createdAt;
}
