package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.domain.entity.RescueCompletionMedia;
import com.uit.petrescueapi.domain.repository.RescueCompletionMediaRepository;
import com.uit.petrescueapi.infrastructure.persistence.mapper.RescueCompletionMediaEntityMapper;
import com.uit.petrescueapi.infrastructure.persistence.repository.RescueCompletionMediaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class RescueCompletionMediaRepositoryAdapter
        implements RescueCompletionMediaRepository {

    private final RescueCompletionMediaJpaRepository jpa;
    private final RescueCompletionMediaEntityMapper mapper;

    @Override
    public RescueCompletionMedia save(
            RescueCompletionMedia media) {

        return mapper.toDomain(
                jpa.save(
                        mapper.toEntity(media)
                )
        );
    }
}
