package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.dto.organization.OrganizationMinimalDto;
import com.uit.petrescueapi.application.dto.pet.PetOwnerSummaryDto;
import com.uit.petrescueapi.application.dto.pet.PetSummaryResponseDto;
import com.uit.petrescueapi.application.port.out.PetSearchPort;
import com.uit.petrescueapi.application.port.out.CloudStoragePort;
import com.uit.petrescueapi.infrastructure.persistence.projection.PetSummaryProjection;
import com.uit.petrescueapi.infrastructure.persistence.repository.PetQueryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetSearchAdapter implements PetSearchPort {

    private final PetQueryJpaRepository queryRepo;
    private final CloudStoragePort cloudStoragePort;

    @Override
    public Page<PetSummaryResponseDto> searchPets(String searchName,
                                                  String species,
                                                  String breed,
                                                  String gender,
                                                  List<String> statuses,
                                                  UUID ownerUserId,
                                                  UUID ownerOrganizationId,
                                                  Pageable pageable) {
        // For now the adapter uses DB LIKE search. A future implementation
        // can replace this bean to call an external search engine (Elasticsearch, etc.)
        if (statuses == null || statuses.isEmpty()) {
            return queryRepo.findAllWithFilters(species, breed, gender, searchName, ownerUserId, ownerOrganizationId, pageable)
                .map(this::toSummaryDto);
        }

        return queryRepo.findAllWithFiltersAndStatuses(species, breed, gender, searchName, statuses, ownerUserId, ownerOrganizationId, pageable)
            .map(this::toSummaryDto);
    }

    private PetSummaryResponseDto toSummaryDto(PetSummaryProjection p) {
        String imageUrl = p.getImagePublicId() != null
                ? cloudStoragePort.buildUrl(p.getImagePublicId())
                : null;

        return PetSummaryResponseDto.builder()
                .petId(p.getId())
                .name(p.getName())
                .speciesId(p.getSpeciesId())
                .species(p.getSpecies())
                .breedId(p.getBreedId())
                .breed(p.getBreed())
                .age(p.getAge())
                .ageDisplay(formatAge(p.getAge()))
                .vaccinated(p.getVaccinated())
                .gender(p.getGender())
                .status(p.getStatus())
                .healthStatus(p.getHealthStatus())
                .owner(toOwnerDto(
                        p.getOwnerType(),
                        p.getOwnerId(),
                        p.getOwnerName(),
                        p.getOwnerAvatarUrl(),
                        p.getOwnerPhone()
                ))
                .imageUrl(imageUrl)
                .organization(p.getOrganizationId() != null ? OrganizationMinimalDto.builder()
                        .organizationId(p.getOrganizationId())
                        .name(p.getOrganizationName())
                        .url(p.getOrganizationUrl())
                        .build() : null)
                .province(p.getProvinceName())
                .ward(p.getWardName())
                .build();
    }

    private PetOwnerSummaryDto toOwnerDto(
            String ownerType,
            UUID ownerId,
            String ownerName,
            String ownerAvatarUrl,
            String ownerPhone
    ) {
        if (ownerId == null && ownerType == null) {
            return null;
        }

        return PetOwnerSummaryDto.builder()
                .ownerType(ownerType)
                .ownerId(ownerId)
                .name(ownerName)
                .avatarUrl(ownerAvatarUrl)
                .phone(ownerPhone)
                .build();
    }

    private String formatAge(Integer ageInMonths) {
        if (ageInMonths == null) return null;
        if (ageInMonths < 12) {
            return ageInMonths + " month" + (ageInMonths != 1 ? "s" : "");
        }
        int years = ageInMonths / 12;
        int months = ageInMonths % 12;
        if (months == 0) {
            return years + " year" + (years != 1 ? "s" : "");
        }
        return years + " year" + (years != 1 ? "s" : "") + " " + months + " month" + (months != 1 ? "s" : "");
    }
}
