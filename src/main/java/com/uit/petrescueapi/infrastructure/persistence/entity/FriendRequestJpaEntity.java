package com.uit.petrescueapi.infrastructure.persistence.entity;

import com.uit.petrescueapi.domain.valueobject.FriendRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "friend_requests", indexes = {
        @Index(name = "idx_friend_request_addressee_status", columnList = "addressee_id, status"),
        @Index(name = "idx_friend_request_requester", columnList = "requester_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequestJpaEntity extends BaseJpaEntity {

    @Id
    @Column(name = "request_id", updatable = false, nullable = false)
    private UUID requestId;

    @Column(name = "requester_id", nullable = false)
    private UUID requesterId;

    @Column(name = "addressee_id", nullable = false)
    private UUID addresseeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FriendRequestStatus status;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
}
