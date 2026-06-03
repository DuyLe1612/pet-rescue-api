package com.uit.petrescueapi.domain.repository;

import com.uit.petrescueapi.domain.entity.RescueCompletionMedia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RescueCompletionMediaRepository {

    RescueCompletionMedia save(
            RescueCompletionMedia media
    );

    List<RescueCompletionMedia> findByCompletionId(
            UUID completionId
    );

    void deleteByCompletionId(
            UUID completionId
    );
}
