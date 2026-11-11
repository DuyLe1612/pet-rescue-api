package com.uit.petrescueapi.application.port.query;

import com.uit.petrescueapi.application.dto.matching.AdopterProfileDto;
import com.uit.petrescueapi.application.dto.matching.MatchResultDto;
import com.uit.petrescueapi.application.dto.matching.PetTraitsDto;
import com.uit.petrescueapi.application.dto.pet.PetSummaryResponseDto;
import com.uit.petrescueapi.domain.entity.AdopterProfile;
import com.uit.petrescueapi.domain.entity.PetTraits;
import com.uit.petrescueapi.domain.valueobject.MatchResult;
import com.uit.petrescueapi.domain.valueobject.SpeciesKind;
import org.springframework.data.domain.Page;

import java.util.Optional;
import java.util.UUID;

public interface MatchingQueryPort {

    AdopterProfileDto getAdopterProfile(UUID userId);

    /** An empty record when nobody described the pet yet. */
    PetTraitsDto getPetTraits(UUID petId);

    /** The caller's own match with a pet. Fails with ADOPTER_PROFILE_REQUIRED until they have a profile. */
    MatchResultDto matchForPet(UUID petId, UUID userId);

    /** AVAILABLE pets for this user, best match first, pets with a warning last. */
    Page<PetSummaryResponseDto> rankAvailable(String species, String breed, String gender, UUID organizationId,
                                              String province, UUID userId, int page, int pageSize);

    /** Everything an application needs to freeze. Empty when the applicant has no profile. */
    Optional<Evaluation> evaluate(UUID userId, UUID petId);

    record Evaluation(AdopterProfile adopterProfile, PetTraits petTraits, SpeciesKind species, MatchResult result) {
    }
}
