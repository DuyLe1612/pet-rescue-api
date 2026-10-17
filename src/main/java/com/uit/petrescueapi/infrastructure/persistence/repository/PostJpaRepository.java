package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.PostJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link PostJpaEntity}.
 * Used by the <b>command-side</b> adapter for post management.
 *
 * <p>For read/query-side operations, see {@link PostQueryJpaRepository}.</p>
 */
@Repository
public interface PostJpaRepository extends JpaRepository<PostJpaEntity, UUID> {

    @Modifying
    @Query("UPDATE PostJpaEntity p SET p.likeCount = p.likeCount + 1 WHERE p.postId = :postId AND p.deleted = false")
    void incrementLikeCount(@Param("postId") UUID postId);

    @Modifying
    @Query("UPDATE PostJpaEntity p SET p.likeCount = CASE WHEN p.likeCount > 0 THEN p.likeCount - 1 ELSE 0 END WHERE p.postId = :postId AND p.deleted = false")
    void decrementLikeCount(@Param("postId") UUID postId);

    @Modifying
    @Query("UPDATE PostJpaEntity p SET p.commentCount = p.commentCount + 1 WHERE p.postId = :postId AND p.deleted = false")
    void incrementCommentCount(@Param("postId") UUID postId);
}
