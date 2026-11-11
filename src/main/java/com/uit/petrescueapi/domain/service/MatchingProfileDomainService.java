package com.uit.petrescueapi.domain.service;

import com.uit.petrescueapi.domain.entity.AdopterProfile;
import com.uit.petrescueapi.domain.entity.PetTraits;
import com.uit.petrescueapi.domain.exception.BusinessException;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.repository.AdopterProfileRepository;
import com.uit.petrescueapi.domain.repository.PetTraitsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/** Keeps the two inputs of matching: the adopter's own profile and what is recorded about a pet. */
@Service
@RequiredArgsConstructor
@Transactional
public class MatchingProfileDomainService {

    private final AdopterProfileRepository adopterProfiles;
    private final PetTraitsRepository petTraits;
    private final AccessControlDomainService accessControl;

    @Transactional(readOnly = true)
    public Optional<AdopterProfile> findAdopterProfile(UUID userId) {
        return adopterProfiles.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public AdopterProfile getAdopterProfile(UUID userId) {
        return findAdopterProfile(userId)
                .orElseThrow(() -> new ResourceNotFoundException("AdopterProfile", "userId", userId));
    }

    /** Organizations read this profile when reviewing an application, so saving needs explicit consent. */
    public AdopterProfile saveAdopterProfile(UUID userId, AdopterProfile profile, boolean consent) {
        if (!consent) {
            throw new BusinessException("Consent is required to save an adopter profile", "ADOPTER_PROFILE_CONSENT_REQUIRED");
        }
        LocalDateTime now = LocalDateTime.now();
        profile.setUserId(userId);
        profile.setConsentAt(findAdopterProfile(userId).map(AdopterProfile::getConsentAt).orElse(now));
        profile.setUpdatedAt(now);
        return adopterProfiles.save(profile);
    }

    /** A pet that exists but has no record yet reads as all-unknown. */
    @Transactional(readOnly = true)
    public PetTraits getPetTraits(UUID petId) {
        requirePet(petId);
        return petTraits.findByPetId(petId).orElseGet(() -> PetTraits.unknown(petId));
    }

    public PetTraits savePetTraits(PetTraits traits, UUID requesterId) {
        requirePet(traits.getPetId());
        accessControl.requirePetManager(traits.getPetId(), requesterId);
        traits.setUpdatedBy(requesterId);
        traits.setUpdatedAt(LocalDateTime.now());
        return petTraits.save(traits);
    }

    /** Species name of an existing pet; 404 otherwise. */
    @Transactional(readOnly = true)
    public String requirePet(UUID petId) {
        return petTraits.findSpeciesName(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", "id", petId));
    }
}
