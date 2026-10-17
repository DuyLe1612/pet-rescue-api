


# Pet Rescue API

A **production-grade REST API** for managing pet rescue operations — connecting rescued animals with adopters through shelters and vet centers. Built with **Clean Architecture**, **CQRS**, and **JWT authentication** on Spring Boot 3.5 / Java 21.

---

## What This App Does

Pet Rescue API is the backend for a pet rescue platform where:

- **Shelters and vet centers** register rescued animals (dogs, cats, etc.) with photos, medical status, and location
- **Users** browse available pets, view details, and submit adoption applications
- **Organization staff** manage their animals, process adoptions, and track rescue cases
- **Admins** oversee the platform, manage roles, and moderate content

### Core Features (Implemented)

| Feature | Status | Description |
|---------|--------|------------|
| **Pet Management** | ✅ Full CRUD | Create, update, delete, status transitions (AVAILABLE → PENDING → ADOPTED, etc.) |
| **Authentication** | ✅ Complete | Register, login, JWT access/refresh tokens, email verification, logout |
| **RBAC Authorization** | ✅ Complete | Role-based access (USER, ADMIN, MEMBER) with Spring Security |
| **Organization Profiles** | ✅ Domain + Data | Shelters/vet centers linked to pets via JOIN queries |
| **Pet Query (CQRS)** | ✅ Optimized | Dedicated read path with LEFT JOIN projections — zero N+1 queries |
| **Pets by Organization** | ✅ Complete | Fetch paged pets via `pet_ownerships` join table (ownership-based, not FK) |

### Planned Features (Mock API Ready)

Swagger UI shows the full planned API surface with mock endpoints for: **Adoptions**, **Posts**, **Rescue Cases**, **Media Upload**, **Tags**, **User Profiles**, and **Role Management**.

---

## Why This Architecture?

This project demonstrates **enterprise-grade patterns** commonly used in real-world Java microservices:

| Pattern | Why It Matters |
|---------|---------------|
| **Clean Architecture** | Business logic is framework-independent — swap Spring for Quarkus without touching domain code |
| **CQRS** | Read and write paths are separated — queries bypass domain validation for direct DB-to-DTO mapping |
| **Ports & Adapters** | Domain defines interfaces; infrastructure provides implementations — testable without a database |
| **Interface Projections** | Spring Data projections fetch only needed columns via JOIN — no ORM overhead, no N+1 |
| **JWT + Refresh Token Rotation** | Stateless auth with secure token lifecycle — industry-standard for SPAs and mobile apps |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     PRESENTATION LAYER                          │
│  Controllers (REST)  ·  GlobalExceptionAdvice  ·  ApiResponse   │
├────────────────────────────┬────────────────────────────────────┤
│      COMMAND PATH          │           QUERY PATH (CQRS)        │
│                            │                                    │
│  PetCommandPort            │  PetQueryPort                      │
│       ↓                    │       ↓                            │
│  PetCommandUseCase         │  PetQueryUseCase                   │
│       ↓                    │       ↓                            │
│  PetDomainService          │  PetQueryDataPort (output port)    │
│  (business rules,          │       ↓                            │
│   @Transactional)          │  PetQueryAdapter                   │
│       ↓                    │  (projection → DTO mapping)        │
│  PetRepository (port)      │       ↓                            │
│       ↓                    │  PetQueryJpaRepository             │
│  PetRepositoryAdapter      │  (LEFT JOIN JPQL queries)          │
│       ↓                    │                                    │
│  PetJpaRepository          │                                    │
├────────────────────────────┴────────────────────────────────────┤
│                    INFRASTRUCTURE LAYER                         │
│  JPA Entities  ·  MapStruct Mappers  ·  Security  ·  Email      │
├─────────────────────────────────────────────────────────────────┤
│                      DOMAIN LAYER (pure Java)                   │
│  Entities  ·  Value Objects  ·  Repository Ports  ·  Services   │
└─────────────────────────────────────────────────────────────────┘
```

### Key Insight: Query Path Bypasses Domain

The **command path** flows through the domain service where business rules (status transitions, validation) are enforced. The **query path** goes directly from the application layer to an infrastructure adapter that executes optimized JOIN queries and returns DTOs — no domain objects, no entity mapping overhead.

---

## Tech Stack

| Technology | Version | Purpose |
|-----------|---------|---------|
| **Spring Boot** | 3.5.10 | Application framework |
| **Java** | 21 (toolchain) | Language |
| **Spring Data JPA** | — | Repository abstraction + interface projections |
| **PostgreSQL** | 16 | Database (Docker for local, NeonDB for prod) |
| **Spring Security** | — | JWT stateless authentication + RBAC |
| **jjwt** | 0.12.6 | HMAC-SHA JWT token generation/validation |
| **Flyway** | — | Database migrations |
| **MapStruct** | 1.6.3 | Compile-time DTO/entity mapping |
| **Lombok** | — | Boilerplate reduction |
| **springdoc-openapi** | 2.8.5 | Swagger UI + OpenAPI spec |
| **Spring Boot Actuator** | — | Health & info endpoints |
| **Docker + Compose** | — | Containerized local dev and deployment |

---

## Project Structure

```
src/main/java/com/uit/petrescueapi/
│
├── domain/                                 # ① DOMAIN LAYER (pure Java + Lombok only)
│   ├── entity/
│   │   ├── BaseEntity.java                 #   Audit fields: createdAt/By, updatedAt/By, deleted/At/By
│   │   ├── Pet.java                        #   Aggregate root — 20+ fields, imageUrls, shelterId
│   │   ├── User.java                       #   verifyEmail(), deactivate(), ban(), hasRole()
│   │   ├── Role.java                       #   id, code, name, description
│   │   ├── Organization.java               #   Shelter/vet center with address, coordinates, status
│   │   ├── RefreshToken.java               #   Token rotation: isExpired(), isUsable(), revoke()
│   │   └── EmailVerificationToken.java     #   One-time token: isExpired(), isUsable(), markUsed()
│   ├── valueobject/
│   │   ├── PetStatus.java                  #   AVAILABLE | ADOPTED | PENDING | FOSTERED | UNAVAILABLE
│   │   ├── Gender.java                     #   MALE | FEMALE | UNKNOWN
│   │   ├── HealthStatus.java               #   HEALTHY | UNDER_TREATMENT | RECOVERING | SPECIAL_NEEDS
│   │   ├── UserStatus.java                 #   PENDING_VERIFICATION | ACTIVE | INACTIVE | BANNED
│   │   ├── SystemRole.java                 #   USER | ADMIN | MEMBER
│   │   └── OrganizationRole.java           #   OWNER | STAFF | VET
│   ├── repository/                         #   Domain ports (interfaces only)
│   │   ├── PetRepository.java              #   save, findById, findAll, findByStatus (paginated)
│   │   ├── UserRepository.java             #   findById, findByEmail, findByUsername, existsBy*
│   │   ├── RoleRepository.java             #   findByCode
│   │   ├── OrganizationRepository.java     #   findAll, findById, findAllByIds (batch)
│   │   ├── RefreshTokenRepository.java     #   save, findByToken, revokeAllByUserId
│   │   └── EmailVerificationTokenRepository.java
│   ├── service/
│   │   ├── PetDomainService.java           #   ⭐ ALLOWED_TRANSITIONS map, CRUD, @Transactional
│   │   ├── UserDomainService.java          #   findById/ByEmail, updateProfile
│   │   └── AuthDomainService.java          #   register, email verification, refresh token rotation
│   └── exception/
│       ├── BaseException.java              #   Abstract (HttpStatus + errorCode)
│       ├── ResourceNotFoundException.java  #   404
│       ├── ResourceAlreadyExistsException.java
│       ├── BusinessException.java          #   400
│       ├── UnauthorizedException.java      #   401
│       └── ForbiddenException.java         #   403
│
├── application/                            # ② APPLICATION LAYER
│   ├── port/
│   │   ├── command/                        #   Write port interfaces (11 total)
│   │   │   ├── PetCommandPort.java         #   create, update, delete, changeStatus
│   │   │   ├── AuthCommandPort.java        #   register, login, refresh, verifyEmail, logout
│   │   │   └── ... (9 more for future features)
│   │   ├── query/                          #   Read port interfaces (10 total, return DTOs)
│   │   │   ├── PetQueryPort.java           #   findById → PetResponseDto, findAllWithFilters/findAvailableWithFilters → Page<Summary>
│   │   │   └── ... (9 more for future features)
│   │   └── out/
│   │       └── PetQueryDataPort.java       #   ⭐ Output port for CQRS query adapter 
│   ├── usecase/
│   │   ├── PetCommandUseCase.java          #   Implements PetCommandPort → delegates to PetDomainService
│   │   ├── PetQueryUseCase.java            #   Implements PetQueryPort → delegates to PetQueryDataPort
│   │   └── AuthCommandUseCase.java         #   Orchestrates: domain + JWT + email
│   └── dto/
│       ├── pet/                            #   Create*, Update*, PetResponseDto, PetSummaryResponseDto
│       ├── auth/                           #   Register*, Login*, RefreshToken*, AuthTokenResponseDto
│       ├── user/                           #   UserResponseDto, UserSummaryResponseDto
│       ├── organization/                   #   5 DTOs (Create*, Response*, Summary*, Member*, AddMember*)
│       └── ... (adoption, post, rescue, role, tag, media DTOs)
│
├── infrastructure/                         # ③ INFRASTRUCTURE LAYER
│   ├── persistence/
│   │   ├── entity/                         #   JPA entities (@Entity, @Table, audit listeners)
│   │   │   ├── PetJpaEntity.java           #   @ElementCollection for imageUrls, 3 indexes
│   │   │   ├── UserJpaEntity.java          #   @ManyToMany roles via user_roles join table
│   │   │   └── ... (Role, Organization, RefreshToken, EmailVerificationToken)
│   │   ├── repository/                     #   Spring Data JPA repositories
│   │   │   ├── PetJpaRepository.java       #   Command-side: findAllNotDeleted, findByStatus
│   │   │   ├── PetQueryJpaRepository.java  #   ⭐ Query-side: LEFT JOIN JPQL projections
│   │   │   └── ... (User, Role, Organization, RefreshToken, EmailVerificationToken)
│   │   ├── mapper/                         #   MapStruct: domain ↔ JPA entity
│   │   │   ├── PetEntityMapper.java
│   │   │   └── ... (User, Role, Organization, RefreshToken, EmailVerificationToken)
│   │   ├── adapter/                        #   Implements domain repository ports
│   │   │   ├── PetRepositoryAdapter.java   #   Command-side adapter
│   │   │   ├── PetQueryAdapter.java        #   ⭐ Query-side: projection → DTO mapping
│   │   │   └── ... (User, Role, Organization, RefreshToken, EmailVerificationToken)
│   │   └── projection/                     #   Spring Data interface projections
│   │       ├── PetSummaryProjection.java   #   Pet fields + org name/type/status (via JOIN)
│   │       └── PetDetailProjection.java    #   All pet fields + org summary (via JOIN)
│   ├── config/
│   │   ├── SecurityConfig.java             #   JWT stateless, BCrypt, public/protected paths
│   │   ├── JpaAuditingConfig.java          #   @EnableJpaAuditing
│   │   ├── JacksonConfig.java              #   JavaTimeModule, no nulls, ISO dates
│   │   ├── OpenApiConfig.java              #   Swagger metadata
│   │   ├── AsyncConfig.java                #   @EnableAsync for email sending
│   │   ├── SwaggerRedirectConfig.java      #   / → /swagger-ui.html
│   │   └── filter/
│   │       ├── CorrelationIdFilter.java    #   X-Correlation-ID → MDC
│   │       └── LoggingFilter.java          #   Request/response logging with latency
│   ├── security/
│   │   ├── JwtService.java                 #   HMAC-SHA, generate/validate tokens, extract claims
│   │   ├── JwtAuthenticationFilter.java    #   OncePerRequestFilter, Bearer token extraction
│   │   ├── JwtAuthenticationEntryPoint.java#   JSON 401 response
│   │   └── CustomUserDetailsService.java   #   Loads UserDetails from DB by UUID or email
│   └── email/
│       └── EmailService.java               #   @Async HTML verification email via JavaMailSender
│
└── presentation/                           # ④ PRESENTATION LAYER
    ├── dto/
    │   ├── ApiResponse.java                #   { success, status, message, data, timestamp, correlationId }
    │   └── PageResponse.java               #   { content, page, size, totalElements, totalPages, last }
    ├── mapper/
    │   └── PetWebMapper.java               #   MapStruct: domain Pet → PetResponseDto (command path only)
    ├── controller/
    │   ├── AuthController.java             #   /api/v1/auth — register, login, refresh, verify, logout, /me
    │   ├── PetController.java              #   /api/v1/pets — CRUD + CQRS queries (7 endpoints)
    │   ├── DevController.java              #   /api/v1/dev — dev-only endpoint (@Profile("development"))
    │   └── mock/                           #   9 mock controllers (Swagger API surface for frontend)
    │       ├── MockAdoptionController.java
    │       ├── MockOrganizationController.java
    │       ├── MockUserController.java
    │       ├── MockPostController.java
    │       ├── MockRescueCaseController.java
    │       ├── MockMediaController.java
    │       ├── MockRoleController.java
    │       ├── MockTagController.java
    │       └── MockPetDetailsController.java
    └── advice/
        └── GlobalExceptionAdvice.java      #   @RestControllerAdvice — 6 handlers, uniform error envelope
```

---

## CQRS Deep Dive

### Why Separate Read and Write?

In a typical CRUD app, both reads and writes go through the same domain service. This works, but:
- **Reads don't need business rules** — they just fetch and transform data
- **Reads can be optimized** with JOIN queries and projections that don't map to domain entities
- **Writes need validation** — status transitions, uniqueness checks, domain invariants

CQRS lets each path be optimized independently.

### Command Path (Writes)

```
Controller
  → PetCommandPort (interface)
    → PetCommandUseCase (DTO → domain mapping)
      → PetDomainService (business rules, @Transactional)
        → PetRepository (domain port)
          → PetRepositoryAdapter (domain ↔ JPA mapping)
            → PetJpaRepository (Spring Data)
```

Business rules enforced here:
- **Status transitions**: `ALLOWED_TRANSITIONS` map — e.g., `AVAILABLE → [PENDING, FOSTERED, UNAVAILABLE]`
- **Soft delete**: Sets `deleted = true`, `deletedAt`, `deletedBy` instead of removing rows
- **Validation**: Bean Validation on request DTOs + domain-level checks

### Query Path (Reads — Bypasses Domain)

```
Controller
  → PetQueryPort (interface, returns DTOs)
    → PetQueryUseCase (thin passthrough)
      → PetQueryDataPort (output port)
        → PetQueryAdapter (projection → DTO mapping)
          → PetQueryJpaRepository (LEFT JOIN JPQL)
            → Interface Projections (PetSummaryProjection, PetDetailProjection)
```

Optimizations:
- **LEFT JOIN** in SQL — organization data fetched in the same query (no N+1)
- **Interface projections** — Spring Data maps only the needed columns, no full entity hydration
- **No domain objects** — results go directly from DB → projection → DTO → controller
- **Separate image query** — for detail view, `@ElementCollection` imageUrls fetched in a second query (unavoidable with JPA collections, but still only 2 queries total)

---

## Authentication & Authorization

### Flow

```
Register → Email Verification → Login → Access Token (JWT) + Refresh Token (DB)
                                          ↓
                               Request with Bearer token
                                          ↓
                               JwtAuthenticationFilter extracts token
                                          ↓
                               JwtService validates signature + expiry
                                          ↓
                               CustomUserDetailsService loads user from DB
                                          ↓
                               SecurityContext set → endpoint accessed
```

### Token Strategy

| Token | Storage | Lifetime | Purpose |
|-------|---------|----------|---------|
| **Access Token** | Client (header) | 15 min (configurable) | Stateless API authentication |
| **Refresh Token** | Database | 7 days (configurable) | Obtain new access token without re-login |

- **Rotation**: Each refresh generates a new refresh token and invalidates the old one
- **Revocation**: Logout revokes all refresh tokens for the user
- **Claims**: `sub` = userId (UUID), `email`, `roles` (list of role codes)

### Public Endpoints (No Auth Required)

```
POST   /api/v1/auth/register
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
GET    /api/v1/auth/verify-email
POST   /api/v1/auth/resend-verification
GET    /api/v1/pets/**
GET    /swagger-ui.html, /api-docs
GET    /actuator/health
```

---

## API Endpoints

### Authentication (`/api/v1/auth`)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/register` | Register new user (sends verification email) |
| `POST` | `/login` | Login → access + refresh tokens |
| `POST` | `/refresh` | Rotate refresh token → new access + refresh tokens |
| `GET` | `/verify-email?token=...` | Verify email address |
| `POST` | `/resend-verification` | Resend verification email |
| `POST` | `/logout` | Revoke all refresh tokens |
| `GET` | `/me` | Get current user profile (requires auth) |

### Pet Management (`/api/v1/pets`)

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/` | Create a new pet |
| `GET` | `/?page=0&pageSize=20` | List pets (filters: `species`, `breed`, `gender`, `status`, `organizationId`, `userId`, `availableOnly`) |
| `GET` | `/{id}` | Get pet detail (with org data + images) |
| `GET` | `/taxonomy` | All species with their breeds (English `name` + Vietnamese `nameVi`) |
| `GET` | `/species` | List species (paginated, search EN/VI) |
| `GET` | `/species/{speciesId}/breeds` | List breeds of a species (paginated, search EN/VI) |
| `PUT` | `/{id}` | Update a pet |
| `PATCH` | `/{id}/status?status=PENDING` | Change pet status (validated transitions) |
| `DELETE` | `/{id}` | Soft-delete a pet |

### Mock Endpoints (9 controllers)

Full Swagger documentation for planned features: Adoptions, Organizations, Users, Posts, Rescue Cases, Media, Roles, Tags, Pet Details (medical records, location, diary).

---

## Quick Start

### Prerequisites
- **JDK 21+** (or use the Gradle wrapper which handles the toolchain)
- **Docker** (for PostgreSQL)

### Run Locally

```bash
# 1. Start PostgreSQL
docker-compose up -d postgres

# 2. Start the app
./gradlew bootRun          # Linux/Mac
gradlew.bat bootRun        # Windows

# App starts on http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### Run with Docker (Full Stack)

```bash
docker-compose up -d       # Starts app + PostgreSQL
docker-compose logs -f app # Follow logs
docker-compose down -v     # Stop + remove volumes
```

### Run Tests

```bash
./gradlew test             # Uses H2 in-memory — no Docker needed
```

---

## Environment Profiles

| Profile | Activated by | Database | Use case |
|---------|-------------|----------|----------|
| *(default)* | `./gradlew bootRun` | Local PostgreSQL (Docker) | Local dev |
| `production` | `SPRING_PROFILES_ACTIVE=production` | NeonDB (cloud Postgres) | Deployment |

---

## Docker

### Local Dev Architecture

```
┌─ docker-compose.yml ─────────────────────┐
│  ┌──────────┐       ┌─────────────────┐  │
│  │ Postgres │◄──────│ Spring Boot App │  │
│  │ :5432    │       │ :8080           │  │
│  └──────────┘       └─────────────────┘  │
└──────────────────────────────────────────┘
```

### Production Architecture

```
┌─ docker-compose.prod.yml ────────────────┐
│  ┌─────────────────┐     ┌────────────┐  │
│  │ Spring Boot App │────►│ NeonDB     │  │
│  │ :8080           │     │ (cloud)    │  │
│  └─────────────────┘     └────────────┘  │
│                    No Postgres container  │
└──────────────────────────────────────────┘
```

**Dockerfile**: Multi-stage build — `eclipse-temurin:21-jdk-alpine` (builder) → `eclipse-temurin:21-jre-alpine` (runtime, non-root user, health check).

---

## CI/CD

Two GitHub Actions workflows in `.github/workflows/`:

| Workflow | Trigger | Steps |
|----------|---------|-------|
| **ci.yml** | Push to `main`/`develop`, PR to `main` | Checkout → JDK 21 → `./gradlew build` → Upload test reports |
| **deploy.yml** | Push to `main` only | Build → Docker image → Push to GHCR → SSH deploy with `docker-compose.prod.yml` |

---

## Conventions

- **URI pattern**: `/api/v1/**` — all responses wrapped in `ApiResponse<T>`
- **`@Transactional`**: Only on domain services — never on adapters or controllers (see [TRANSACTION_BOUNDARIES.md](TRANSACTION_BOUNDARIES.md) for detailed rules)
- **Mapping**: MapStruct for all conversions (domain ↔ JPA entity, domain → DTO)
- **Soft delete**: `deleted`, `deletedAt`, `deletedBy` in `BaseEntity`
- **Pagination**: Every GET list endpoint is paginated (`PageResponse<T>` envelope)
- **Query ports return DTOs**: Not domain entities — keeps the query path free of domain coupling
- **Organization data via JOIN**: Always included in pet queries — no separate enrichment step

---

## CQRS Best Practices & Architecture Rules

### The Golden Rule: Controllers MUST Use Ports, Not Domain Services

**✅ Correct Pattern** (all production controllers follow this):
```java
@RestController
@RequiredArgsConstructor
public class PetController {
    private final PetCommandPort commandPort;  // Write operations
    private final PetQueryPort queryPort;      // Read operations
    private final PetWebMapper mapper;         // Domain → DTO (command path only)

    @PostMapping
    public ResponseEntity<ApiResponse<PetResponseDto>> create(@RequestBody CreatePetRequestDto cmd) {
        // CommandPort returns domain entity → map to DTO in controller
        return ResponseEntity.ok(ApiResponse.ok(mapper.toDto(commandPort.create(cmd))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PetResponseDto>> getById(@PathVariable UUID id) {
        // QueryPort returns DTO directly (no mapping needed)
        return ResponseEntity.ok(ApiResponse.ok(queryPort.findById(id)));
    }
}
```

**❌ Incorrect Pattern** (violates Clean Architecture):
```java
@RestController
@RequiredArgsConstructor
public class BadController {
    private final PetDomainService domainService;  // ❌ Controllers must NOT inject domain services

    @PostMapping
    public ResponseEntity<Pet> create(@RequestBody CreatePetRequestDto cmd) {
        return ResponseEntity.ok(domainService.create(cmd));  // ❌ Exposes domain entity directly
    }
}
```

### Why This Matters

1. **Dependency Inversion** — Controllers depend on abstractions (ports), not implementations (domain services)
2. **Testability** — Mock the port interface, not the concrete service
3. **Flexibility** — Swap implementations without changing controllers
4. **Separation of Concerns** — Controllers orchestrate HTTP; domain services enforce business rules

### Command Path vs Query Path

| Aspect | Command Path (Writes) | Query Path (Reads) |
|--------|----------------------|-------------------|
| **Port returns** | Domain entity | DTO directly |
| **Validation** | Bean Validation + domain rules | None (read-only) |
| **Transaction** | `@Transactional` on domain service | `@Transactional(readOnly = true)` on query adapter |
| **Mapping** | Controller maps entity → DTO via web mapper | Adapter maps projection → DTO |
| **Passes through domain** | ✅ Yes (business rules enforced) | ❌ No (bypasses domain for performance) |

### When CQRS Exceptions Are Acceptable

**Two controllers intentionally bypass CQRS** for valid architectural reasons:

#### 1. `DevController` (Development Utility)
```java
@Profile("dev")  // Only active in development
@RestController
public class DevController {
    private final UserDomainService userDomainService;  // ✅ Acceptable here

    @PostMapping("/create-account")
    public ResponseEntity<ApiResponse<UserResponseDto>> createAccount(@RequestBody CreateAdminRequest req) {
        // Directly creates users with pre-verified email (bypasses normal registration)
        User saved = userDomainService.createUser(req.username, req.email, hashedPassword, req.role);
        return ResponseEntity.ok(ApiResponse.ok(mapToDto(saved)));
    }
}
```

**Why it's OK:** Dev endpoints are operational tools for bootstrapping (creating admins, seeding data). They're never deployed to production and don't represent domain use cases.

#### 2. `AdminController` (Privileged Operations)
```java
@PreAuthorize("hasRole('ADMIN')")
@RestController
public class AdminController {
    private final UserDomainService userDomainService;              // ✅ Acceptable for admin ops
    private final OrganizationDomainService organizationDomainService;  // ✅ Acceptable for admin ops

    @PostMapping("/organizations/{orgId}/accounts")
    public ResponseEntity<ApiResponse<UserResponseDto>> createOrganizationAccount(...) {
        // Creates user with pre-verified email (bypasses normal registration flow)
        User user = userDomainService.createUser(username, email, hashedPassword, systemRole);
        // Force-assigns org role (bypasses normal membership request flow)
        organizationDomainService.addMember(organizationId, user.getId(), orgRole);
        return ResponseEntity.ok(ApiResponse.ok(mapToDto(user)));
    }
}
```

**Why it's OK:** Admin operations are **privileged/tactical** — they bypass normal business flows by design (e.g., force-create users, override permissions). These are maintenance operations, not domain use cases.

### General Guideline

- **Production domain controllers** (Pet, Auth, Organization, Adoption, Post, etc.) → **MUST use CQRS ports**
- **Operational controllers** (Dev, Admin) → **MAY directly use domain services** if the operation is truly outside normal business flows

---

## Guide: How to Add a New CQRS Query Endpoint (Step by Step)

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

---

## License

This project is for educational purposes.
