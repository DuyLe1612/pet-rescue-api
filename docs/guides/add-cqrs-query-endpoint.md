# Guide: add a CQRS query endpoint

A worked example, step by step. The rules behind it are in [Architecture](../architecture.md).

This section walks through **exactly what was done** to implement `GET /api/v1/pets/by-organization/{organizationId}` — fetching paged pets by organization ID using the `pet_ownerships` join table. Use this as a recipe for adding any new query endpoint.

> **Note:** this dedicated endpoint has since been removed in favour of `GET /api/v1/pets?organizationId={id}`; the steps below remain a valid recipe.

### Context

Pets are linked to organizations through a **`pet_ownerships` table** (not a direct FK). This table tracks ownership history:

```sql
CREATE TABLE pet_ownerships (
    pet_id     UUID        NOT NULL REFERENCES pets (id),
    owner_type VARCHAR(20),   -- 'USER' or 'ORGANIZATION'
    owner_id   UUID,
    from_time  TIMESTAMPTZ NOT NULL,
    to_time    TIMESTAMPTZ,   -- NULL = current owner
    PRIMARY KEY (pet_id, from_time)
);
```

### Step 1: Domain Entity

**File**: `domain/entity/PetOwnership.java`

Create a pure Java domain entity (no Spring/JPA annotations — only Lombok):

```java
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PetOwnership {
    private UUID petId;
    private String ownerType;   // "USER" or "ORGANIZATION"
    private UUID ownerId;
    private LocalDateTime fromTime;
    private LocalDateTime toTime;

    public boolean isCurrent() {
        return toTime == null;
    }
}
```

### Step 2: JPA Entity (Infrastructure)

**Files**: `infrastructure/persistence/entity/PetOwnershipJpaEntity.java`, `PetOwnershipId.java`

Map to the existing DB table. Since `pet_ownerships` has a **composite primary key** `(pet_id, from_time)`, we need an `@IdClass`:

```java
@Entity
@Table(name = "pet_ownerships")
@IdClass(PetOwnershipId.class)
public class PetOwnershipJpaEntity {
    @Id @Column(name = "pet_id")   private UUID petId;
    @Id @Column(name = "from_time") private LocalDateTime fromTime;
    @Column(name = "owner_type")    private String ownerType;
    @Column(name = "owner_id")      private UUID ownerId;
    @Column(name = "to_time")       private LocalDateTime toTime;
}
```

```java
public class PetOwnershipId implements Serializable {
    private UUID petId;
    private LocalDateTime fromTime;
}
```

### Step 3: Query in JPA Repository

**File**: `infrastructure/persistence/repository/PetQueryJpaRepository.java`

Add a JPQL query that **joins through `pet_ownerships`** to find pets currently owned by an organization. We reuse the existing `PetSummaryProjection` — no new projection needed:

```java
@Query("""
    SELECT p.id          AS id,
           p.name        AS name,
           p.species     AS species,
           p.breed       AS breed,
           p.age         AS age,
           p.vaccinated  AS vaccinated,
           p.gender      AS gender,
           p.status      AS status,
           p.healthStatus AS healthStatus,
           o.organizationId AS organizationId,
           o.name        AS organizationName,
           o.type        AS organizationType,
           o.status      AS organizationStatus
    FROM PetOwnershipJpaEntity po
    JOIN PetJpaEntity p ON po.petId = p.id
    LEFT JOIN OrganizationJpaEntity o ON p.shelterId = o.organizationId
    WHERE po.ownerId = :organizationId
      AND po.ownerType = 'ORGANIZATION'
      AND po.toTime IS NULL
      AND p.deleted = false
""")
Page<PetSummaryProjection> findSummariesByOrganizationId(
        @Param("organizationId") UUID organizationId,
        Pageable pageable);
```

Key points:
- `po.toTime IS NULL` → only current ownerships
- `po.ownerType = 'ORGANIZATION'` → filter to org-type owners
- `LEFT JOIN OrganizationJpaEntity` → still include org details in the projection
- Reuses `PetSummaryProjection` — same columns, same mapping

### Step 4: Output Port (Application Layer)

**File**: `application/port/out/PetQueryDataPort.java`

Add one method:

```java
Page<PetSummaryResponseDto> findSummariesByOrganizationId(UUID organizationId, Pageable pageable);
```

### Step 5: Adapter (Infrastructure → Maps Projection → DTO)

**File**: `infrastructure/persistence/adapter/PetQueryAdapter.java`

Implement the new port method. The `toSummaryDto()` mapping already exists — just reuse it:

```java
@Override
public Page<PetSummaryResponseDto> findSummariesByOrganizationId(UUID organizationId, Pageable pageable) {
    return queryRepo.findSummariesByOrganizationId(organizationId, pageable).map(this::toSummaryDto);
}
```

### Step 6: Query Port + Use Case (Application Layer)

**File**: `application/port/query/PetQueryPort.java`

```java
Page<PetSummaryResponseDto> findByOrganizationId(UUID organizationId, Pageable pageable);
```

**File**: `application/usecase/PetQueryUseCase.java`

```java
@Override
public Page<PetSummaryResponseDto> findByOrganizationId(UUID organizationId, Pageable pageable) {
    log.debug("Query: find pets by organization id {} (paginated)", organizationId);
    return queryDataPort.findSummariesByOrganizationId(organizationId, pageable);
}
```

### Step 7: Controller Endpoint

**File**: `presentation/controller/PetController.java`

```java
@GetMapping("/by-organization/{organizationId}")
@Operation(summary = "List pets owned by an organization (paginated, via pet_ownerships table)")
public ResponseEntity<ApiResponse<PageResponse<PetSummaryResponseDto>>> getByOrganization(
        @PathVariable UUID organizationId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(ApiResponse.ok(
            PageResponse.from(petQueryPort.findByOrganizationId(organizationId, PageRequest.of(page, size)))));
}
```

### Summary: Files Changed

| # | Layer | File | Action |
|---|-------|------|--------|
| 1 | Domain | `domain/entity/PetOwnership.java` | **Created** — pure Java entity |
| 2 | Infrastructure | `persistence/entity/PetOwnershipJpaEntity.java` | **Created** — JPA entity with `@IdClass` |
| 3 | Infrastructure | `persistence/entity/PetOwnershipId.java` | **Created** — composite PK class |
| 4 | Infrastructure | `persistence/repository/PetQueryJpaRepository.java` | **Modified** — added JOIN query |
| 5 | Application | `port/out/PetQueryDataPort.java` | **Modified** — added method |
| 6 | Infrastructure | `persistence/adapter/PetQueryAdapter.java` | **Modified** — implemented method |
| 7 | Application | `port/query/PetQueryPort.java` | **Modified** — added method |
| 8 | Application | `usecase/PetQueryUseCase.java` | **Modified** — delegated to port |
| 9 | Presentation | `controller/PetController.java` | **Modified** — added endpoint |

### Data Flow

```
GET /api/v1/pets/by-organization/{orgId}?page=0&size=20
    ↓
PetController.getByOrganization()
    ↓
PetQueryPort.findByOrganizationId(orgId, pageable)
    ↓
PetQueryUseCase (passthrough)  
    ↓
PetQueryDataPort.findSummariesByOrganizationId(orgId, pageable)
    ↓
PetQueryAdapter → queryRepo.findSummariesByOrganizationId()
    ↓
JPQL: pet_ownerships JOIN pets LEFT JOIN organizations
    ↓
Page<PetSummaryProjection> → map(toSummaryDto) → Page<PetSummaryResponseDto>
    ↓
PageResponse.from(page) → ApiResponse.ok()
```

