package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.matching.AdopterProfileDto;
import com.uit.petrescueapi.application.dto.matching.PetTraitsDto;
import com.uit.petrescueapi.application.port.command.MatchingCommandPort;
import com.uit.petrescueapi.domain.service.MatchingProfileDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchingCommandUseCase implements MatchingCommandPort {

    private final MatchingProfileDomainService profiles;

    @Override
    public AdopterProfileDto saveAdopterProfile(UUID userId, AdopterProfileDto profile) {
        return AdopterProfileDto.from(profiles.saveAdopterProfile(userId, profile.toDomain(), profile.isConsent()));
    }

    @Override
    public PetTraitsDto savePetTraits(UUID petId, PetTraitsDto traits, UUID requesterId) {
        return PetTraitsDto.from(profiles.savePetTraits(traits.toDomain(petId), requesterId));
    }
}
