package com.uit.petrescueapi.infrastructure.persistence.adapter;

import com.uit.petrescueapi.application.dto.organization.OrganizationMinimalDto;
import com.uit.petrescueapi.application.dto.pet.PetAdminResponseDto;
import com.uit.petrescueapi.application.dto.pet.PetOwnerSummaryAdminDto;
import com.uit.petrescueapi.application.dto.pet.PetOwnerSummaryDto;
import com.uit.petrescueapi.application.dto.pet.PetResponseDto;
import com.uit.petrescueapi.application.dto.pet.PetSummaryResponseDto;
import com.uit.petrescueapi.application.port.out.CloudStoragePort;
import com.uit.petrescueapi.application.port.out.PetQueryDataPort;
import com.uit.petrescueapi.application.port.out.PetSearchPort;
import com.uit.petrescueapi.domain.exception.ResourceNotFoundException;
import com.uit.petrescueapi.domain.valueobject.PetStatus;
import com.uit.petrescueapi.infrastructure.persistence.projection.PetDetailProjection;
import com.uit.petrescueapi.infrastructure.persistence.projection.PetSummaryProjection;
import com.uit.petrescueapi.infrastructure.persistence.repository.PetQueryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Query-side adapter (CQRS read path).
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetQueryAdapter implements PetQueryDataPort {

        private final PetQueryJpaRepository queryRepo;
        private final CloudStoragePort cloudStoragePort;
        private final PetSearchPort petSearchPort;

    // ── List (summary) queries with filters ──────────────────

    @Override
    public Page<PetSummaryResponseDto> findAllWithFilters(
            String species,
            String breed,
            String gender,
            String searchName,
            List<PetStatus> statuses,
            UUID ownerUserId,
            UUID ownerOrganizationId,
            String province,
            Pageable pageable
    ) {
        province = province == null || province.isBlank() ? null : province.trim();
        List<String> statusNames = statuses != null ? statuses.stream().map(PetStatus::name).toList() : null;
        if (searchName != null && !searchName.isBlank()) {
                        return petSearchPort.searchPets(searchName, species, breed, gender, statusNames, ownerUserId, ownerOrganizationId, province, pageable);
        }

                if (statusNames == null || statusNames.isEmpty()) {
                        return queryRepo.findAllWithFilters(
                                        species, breed, gender, null, ownerUserId, ownerOrganizationId, province, pageable
                        ).map(this::toSummaryDto);
                }

                return queryRepo.findAllWithFiltersAndStatuses(
                                species, breed, gender, null, statusNames, ownerUserId, ownerOrganizationId, province, pageable
                ).map(this::toSummaryDto);
    }

    @Override
    public Page<PetSummaryResponseDto> findAvailableWithFilters(
            String species,
            String breed,
            String gender,
            UUID ownerOrganizationId,
            String province,
            Pageable pageable
    ) {
        return queryRepo.findByStatusWithFilters(
                species,
                breed,
                gender,
                PetStatus.AVAILABLE.name(),
                ownerOrganizationId,
                province == null || province.isBlank() ? null : province.trim(),
                pageable
        ).map(this::toSummaryDto);
    }

    // ── Detail (single pet) query ───────────────

    @Override
    public PetResponseDto findById(UUID id) {
        PetDetailProjection proj = queryRepo.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", "id", id));

        List<String> imageUrls = imageUrls(id);

        return toResponseDto(proj, imageUrls.isEmpty() ? null : imageUrls.get(0), imageUrls);
    }

    @Override
    public PetAdminResponseDto findByIdForAdmin(UUID id) {
        PetDetailProjection proj = queryRepo.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", "id", id));

        List<String> imageUrls = imageUrls(id);

        return toAdminResponseDto(proj, imageUrls.isEmpty() ? null : imageUrls.get(0), imageUrls);
    }

    /** Primary image first. */
    private List<String> imageUrls(UUID petId) {
        return queryRepo.findImagePublicIdsById(petId).stream()
                .filter(publicId -> publicId != null)
                .map(cloudStoragePort::buildUrl)
                .toList();
    }

    // ── Projection → DTO mappers ───────────────

    private PetSummaryResponseDto toSummaryDto(PetSummaryProjection p) {
        String imageUrl = p.getImagePublicId() != null 
                ? cloudStoragePort.buildUrl(p.getImagePublicId()) 
                : null;
        
        return PetSummaryResponseDto.builder()
                .petId(p.getId())
                .name(p.getName())
                .speciesId(p.getSpeciesId())
                .species(p.getSpecies())
                .speciesVi(p.getSpeciesVi())
                .breedId(p.getBreedId())
                .breed(p.getBreed())
                .breedVi(p.getBreedVi())
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

        private PetResponseDto toResponseDto(PetDetailProjection p, String primaryImageUrl, List<String> imageUrls) {
        return PetResponseDto.builder()
                .petId(p.getId())
                .name(p.getName())
                .speciesId(p.getSpeciesId())
                .species(p.getSpecies())
                .speciesVi(p.getSpeciesVi())
                .breedId(p.getBreedId())
                .breed(p.getBreed())
                .breedVi(p.getBreedVi())
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
                        "ORGANIZATION".equals(p.getOwnerType()) ? p.getOwnerPhone() : null
                ))
                .organization(p.getOrganizationId() != null ? OrganizationMinimalDto.builder()
                        .organizationId(p.getOrganizationId())
                        .name(p.getOrganizationName())
                        .url(p.getOrganizationUrl())
                        .build() : null)
                .province(p.getProvinceName())
                .ward(p.getWardName())
                .color(p.getColor())
                .weight(p.getWeight())
                .description(p.getDescription())
                .neutered(p.getNeutered())
                .rescueDate(p.getRescueDate())
                .rescueLocation(p.getRescueLocation())
                .rescueCaseId(p.getRescueCaseId())
                .primaryImageUrl(primaryImageUrl)
                .imageUrls(imageUrls)
                .build();
    }

    private PetAdminResponseDto toAdminResponseDto(PetDetailProjection p, String primaryImageUrl, List<String> imageUrls) {
        return PetAdminResponseDto.builder()
                .petId(p.getId())
                .petCode(p.getPetCode())
                .name(p.getName())
                .speciesId(p.getSpeciesId())
                .species(p.getSpecies())
                .speciesVi(p.getSpeciesVi())
                .breedId(p.getBreedId())
                .breed(p.getBreed())
                .breedVi(p.getBreedVi())
                .age(p.getAge())
                .ageDisplay(formatAge(p.getAge()))
                .vaccinated(p.getVaccinated())
                .gender(p.getGender())
                .status(p.getStatus())
                .healthStatus(p.getHealthStatus())
                .owner(toOwnerAdminDto(
                        p.getOwnerType(),
                        p.getOwnerId(),
                        p.getOwnerName(),
                        p.getOwnerAvatarUrl(),
                        p.getOwnerPhone(),
                        p.getCaretakerUserId(),
                        p.getCaretakerName(),
                        p.getCaretakerAvatarUrl(),
                        p.getCaretakerPhone()
                ))
                .organization(p.getOrganizationId() != null ? OrganizationMinimalDto.builder()
                        .organizationId(p.getOrganizationId())
                        .name(p.getOrganizationName())
                        .url(p.getOrganizationUrl())
                        .build() : null)
                .province(p.getProvinceName())
                .ward(p.getWardName())
                .color(p.getColor())
                .weight(p.getWeight())
                .description(p.getDescription())
                .neutered(p.getNeutered())
                .rescueDate(p.getRescueDate())
                .rescueLocation(p.getRescueLocation())
                .rescueCaseId(p.getRescueCaseId())
                .primaryImageUrl(primaryImageUrl)
                .imageUrls(imageUrls)
                .shelterId(p.getShelterId())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
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

    private PetOwnerSummaryAdminDto toOwnerAdminDto(
            String ownerType,
            UUID ownerId,
            String ownerName,
            String ownerAvatarUrl,
            String ownerPhone,
            UUID caretakerUserId,
            String caretakerName,
            String caretakerAvatarUrl,
            String caretakerPhone
    ) {
        if (ownerId == null && ownerType == null) {
            return null;
        }

        return PetOwnerSummaryAdminDto.builder()
                .ownerType(ownerType)
                .ownerId(ownerId)
                .name(ownerName)
                .avatarUrl(ownerAvatarUrl)
                .phone(ownerPhone)
                .caretakerUserId(caretakerUserId)
                .caretakerName(caretakerName)
                .caretakerAvatarUrl(caretakerAvatarUrl)
                .caretakerPhone(caretakerPhone)
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
