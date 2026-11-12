package com.uit.petrescueapi.application.port.command;

import com.uit.petrescueapi.domain.entity.RescueCase;

import java.util.Optional;
import java.util.UUID;

public interface RescueCaseSharePort {

    /**
     * Publishes a community post about a case that was just reported. The case is already saved and
     * must not depend on this: any failure is swallowed and reported as an empty result.
     */
    Optional<UUID> shareAsPost(RescueCase rescueCase, String content, UUID authorId);
}
