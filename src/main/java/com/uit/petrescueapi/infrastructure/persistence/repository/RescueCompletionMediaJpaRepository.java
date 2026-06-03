package com.uit.petrescueapi.infrastructure.persistence.repository;

import com.uit.petrescueapi.infrastructure.persistence.entity.RescueCompletionMediaId;
import com.uit.petrescueapi.infrastructure.persistence.entity.RescueCompletionMediaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RescueCompletionMediaJpaRepository
        extends JpaRepository<
                RescueCompletionMediaJpaEntity,
                RescueCompletionMediaId> {

    @Query("""
        select m
        from RescueCompletionMediaJpaEntity m
        where m.id.completionId = :completionId
    """)
    List<RescueCompletionMediaJpaEntity> findByCompletionId(
            UUID completionId
    );

    @Modifying
    @Query("""
        delete
        from RescueCompletionMediaJpaEntity m
        where m.id.completionId = :completionId
    """)
    void deleteByCompletionId(
            UUID completionId
    );
}
