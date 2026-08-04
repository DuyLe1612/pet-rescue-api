package com.uit.petrescueapi.infrastructure.persistence.entity;

import com.uit.petrescueapi.domain.valueobject.MedicalRecordType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA entity mapped to the {@code pet_medical_records} table.
 * Extends BaseJpaEntity — {@code created_by} serves as the medical record creator.
 */
@Entity
@Table(name = "pet_medical_records", indexes = {
        @Index(name = "idx_pet_medical_pet", columnList = "pet_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PetMedicalRecordJpaEntity extends BaseJpaEntity {

    @Id
    @Column(name = "record_id", updatable = false, nullable = false)
    private UUID recordId;

    @Column(name = "pet_id", nullable = false)
    private UUID petId;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_type", nullable = false, length = 30)
    private MedicalRecordType recordType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "vaccine", length = 255)
    private String vaccine;

    @Column(name = "diagnosis", columnDefinition = "TEXT")
    private String diagnosis;

    @Column(name = "record_date")
    private LocalDateTime recordDate;

    /**
     * Read-only association used by JOIN queries only.
     * Persistence is handled by the {@code petId} field above.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", insertable = false, updatable = false)
    private PetJpaEntity pet;
}
