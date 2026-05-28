package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.domain.valueobject.FriendRequestStatus;
import com.uit.petrescueapi.infrastructure.persistence.entity.FriendRequestJpaEntity;
import com.uit.petrescueapi.infrastructure.persistence.projection.FriendSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendRequestJpaRepository extends JpaRepository<FriendRequestJpaEntity, UUID> {

    Optional<FriendRequestJpaEntity> findByRequesterIdAndAddresseeIdAndStatus(UUID requesterId, UUID addresseeId, FriendRequestStatus status);

    Optional<FriendRequestJpaEntity> findByRequesterIdAndAddresseeId(UUID requesterId, UUID addresseeId);

    @Query("SELECT fr FROM FriendRequestJpaEntity fr WHERE fr.addresseeId = :userId AND fr.status = :status")
    Page<FriendRequestJpaEntity> findByAddresseeAndStatus(@Param("userId") UUID userId,
                                                          @Param("status") FriendRequestStatus status,
                                                          Pageable pageable);

        @Query("""
          SELECT fr
          FROM FriendRequestJpaEntity fr
          JOIN UserJpaEntity requester ON requester.userId = fr.requesterId
          WHERE fr.addresseeId = :userId
            AND fr.status = :status
            AND (:search IS NULL OR :search = '' OR
           LOWER(requester.username) LIKE LOWER(CONCAT('%', :search, '%')) OR
           LOWER(requester.fullName) LIKE LOWER(CONCAT('%', :search, '%')))
          """)
        Page<FriendRequestJpaEntity> findByAddresseeAndStatus(@Param("userId") UUID userId,
                    @Param("status") FriendRequestStatus status,
                    @Param("search") String search,
                    Pageable pageable);

    @Query("SELECT fr FROM FriendRequestJpaEntity fr WHERE fr.requesterId = :userId AND fr.status = :status")
    Page<FriendRequestJpaEntity> findByRequesterAndStatus(@Param("userId") UUID userId,
                                                          @Param("status") FriendRequestStatus status,
                                                          Pageable pageable);

    @Query(value = """
            SELECT u.user_id AS userId,
                   u.username AS username,
                   u.full_name AS fullName,
                   u.avatar_url AS avatarUrl
            FROM friend_requests fr
            JOIN users u
              ON u.user_id = CASE
                  WHEN fr.requester_id = :userId THEN fr.addressee_id
                  ELSE fr.requester_id
              END
            WHERE (fr.requester_id = :userId OR fr.addressee_id = :userId)
              AND fr.status = 'ACCEPTED'
              AND fr.is_deleted = false
            AND (:search IS NULL OR :search = '' OR
                 LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR
                 LOWER(u.full_name) LIKE LOWER(CONCAT('%', :search, '%')))
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM friend_requests fr
            JOIN users u
              ON u.user_id = CASE
                  WHEN fr.requester_id = :userId THEN fr.addressee_id
                  ELSE fr.requester_id
              END
            WHERE (fr.requester_id = :userId OR fr.addressee_id = :userId)
              AND fr.status = 'ACCEPTED'
              AND fr.is_deleted = false
              AND (:search IS NULL OR :search = '' OR
                   LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR
                   LOWER(u.full_name) LIKE LOWER(CONCAT('%', :search, '%')))
            """,
            nativeQuery = true)
    Page<FriendSummaryProjection> findFriendsByUser(@Param("userId") UUID userId, @Param("search") String search, Pageable pageable);
}
