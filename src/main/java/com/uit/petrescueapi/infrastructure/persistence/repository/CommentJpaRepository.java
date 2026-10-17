package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.CommentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CommentJpaRepository extends JpaRepository<CommentJpaEntity, UUID> {

    @Modifying
    @Query("UPDATE CommentJpaEntity c SET c.likeCount = c.likeCount + 1 WHERE c.commentId = :commentId AND c.deleted = false")
    void incrementLikeCount(@Param("commentId") UUID commentId);

    @Modifying
    @Query("UPDATE CommentJpaEntity c SET c.likeCount = CASE WHEN c.likeCount > 0 THEN c.likeCount - 1 ELSE 0 END WHERE c.commentId = :commentId AND c.deleted = false")
    void decrementLikeCount(@Param("commentId") UUID commentId);

    @Modifying
    @Query("UPDATE CommentJpaEntity c SET c.replyCount = c.replyCount + 1 WHERE c.commentId = :commentId AND c.deleted = false")
    void incrementReplyCount(@Param("commentId") UUID commentId);
}
