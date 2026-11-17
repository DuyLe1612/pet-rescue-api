package com.uit.petrescueapi.application.usecase;

import com.uit.petrescueapi.application.dto.matching.AdopterProfileDto;
import com.uit.petrescueapi.application.dto.matching.MatchResultDto;
import com.uit.petrescueapi.application.dto.matching.PetTraitsDto;
import com.uit.petrescueapi.application.dto.pet.PetSummaryResponseDto;
import com.uit.petrescueapi.application.port.out.PetQueryDataPort;
import com.uit.petrescueapi.application.port.query.MatchingQueryPort;
import com.uit.petrescueapi.domain.entity.AdopterProfile;
import com.uit.petrescueapi.domain.entity.PetTraits;
import com.uit.petrescueapi.domain.exception.UnprocessableEntityException;
import com.uit.petrescueapi.domain.repository.PetTraitsRepository;
import com.uit.petrescueapi.domain.service.MatchingProfileDomainService;
import com.uit.petrescueapi.domain.service.MatchingService;
import com.uit.petrescueapi.domain.valueobject.MatchLevel;
import com.uit.petrescueapi.domain.valueobject.MatchResult;
import com.uit.petrescueapi.domain.valueobject.SpeciesKind;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchingQueryUseCase implements MatchingQueryPort {

    /**
     * Ranking is computed in memory, so the candidate set is bounded. The container has 512 MB;
     * a summary row is small, and filters (species, province, organization) narrow it first.
     */
    static final int MAX_CANDIDATES = 500;

    private final MatchingProfileDomainService profiles;
    private final MatchingService matching;
    private final PetTraitsRepository petTraits;
    private final PetQueryDataPort petQuery;

    @Override
    public AdopterProfileDto getAdopterProfile(UUID userId) {
        return AdopterProfileDto.from(profiles.getAdopterProfile(userId));
    }

    @Override
    public PetTraitsDto getPetTraits(UUID petId) {
        return PetTraitsDto.from(profiles.getPetTraits(petId));
    }

    @Override
    public MatchResultDto matchForPet(UUID petId, UUID userId) {
        String species = profiles.requirePet(petId);
        AdopterProfile adopter = requireProfile(userId);
        PetTraits traits = petTraits.findByPetId(petId).orElse(null);
        return MatchResultDto.forAdopter(matching.evaluate(adopter, SpeciesKind.fromName(species), traits));
    }

    @Override
    public Page<PetSummaryResponseDto> rankAvailable(String species, String breed, String gender, UUID organizationId,
                                                     String province, UUID userId, int page, int pageSize) {
        AdopterProfile adopter = requireProfile(userId);
        List<PetSummaryResponseDto> candidates = petQuery.findAvailableWithFilters(species, breed, gender, organizationId,
                province, PageRequest.of(0, MAX_CANDIDATES, Sort.by(Sort.Direction.DESC, "created_at"))).getContent();
        Map<UUID, PetTraits> traits = petTraits.findByPetIds(candidates.stream().map(PetSummaryResponseDto::getPetId).toList());

        List<Ranked> ranked = candidates.stream()
                .map(pet -> new Ranked(pet, matching.evaluate(adopter, SpeciesKind.fromName(pet.getSpecies()), traits.get(pet.getPetId()))))
                .sorted(BEST_FIRST)
                .toList();

        PageRequest pageable = PageRequest.of(page, pageSize);
        List<PetSummaryResponseDto> content = ranked.stream()
                .skip(pageable.getOffset())
                .limit(pageSize)
                .map(r -> {
                    r.pet().setMatch(MatchResultDto.forAdopter(r.result()));
                    return r.pet();
                })
                .toList();
        return new PageImpl<>(content, pageable, ranked.size());
    }

    @Override
    public Optional<Evaluation> evaluate(UUID userId, UUID petId) {
        return profiles.findAdopterProfile(userId).map(adopter -> {
            SpeciesKind species = SpeciesKind.fromName(profiles.requirePet(petId));
            PetTraits traits = petTraits.findByPetId(petId).orElseGet(() -> PetTraits.unknown(petId));
            return new Evaluation(adopter, traits, species, matching.evaluate(adopter, species, traits));
        });
    }

    private AdopterProfile requireProfile(UUID userId) {
        return profiles.findAdopterProfile(userId).orElseThrow(() -> new UnprocessableEntityException(
                "Fill in your adopter profile first", "ADOPTER_PROFILE_REQUIRED"));
    }

    private record Ranked(PetSummaryResponseDto pet, MatchResult result) {
        double score() {
            return result.score() == null ? -1 : result.score();
        }
    }

    /** Pets with a warning go last; the pet id breaks ties so paging is stable. */
    private static final Comparator<Ranked> BEST_FIRST = Comparator
            .comparing((Ranked r) -> r.result().level() == MatchLevel.NOT_SUITABLE)
            .thenComparing(Comparator.comparingDouble(Ranked::score).reversed())
            .thenComparing(r -> r.pet().getPetId());
}
