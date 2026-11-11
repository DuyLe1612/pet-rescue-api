package com.uit.petrescueapi.application.port.command;

import com.uit.petrescueapi.application.dto.matching.AdopterProfileDto;
import com.uit.petrescueapi.application.dto.matching.PetTraitsDto;

import java.util.UUID;

public interface MatchingCommandPort {

    AdopterProfileDto saveAdopterProfile(UUID userId, AdopterProfileDto profile);

    PetTraitsDto savePetTraits(UUID petId, PetTraitsDto traits, UUID requesterId);
}
