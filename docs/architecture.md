# Architecture

How the backend is put together, and the rules every change follows. Back to the [README](../README.md).

## Why This Architecture?

This project demonstrates **enterprise-grade patterns** commonly used in real-world Java microservices:

| Pattern | Why It Matters |
|---------|---------------|
| **Clean Architecture** | Business logic is framework-independent — swap Spring for Quarkus without touching domain code |
| **CQRS** | Read and write paths are separated — queries bypass domain validation for direct DB-to-DTO mapping |
| **Ports & Adapters** | Domain defines interfaces; infrastructure provides implementations — testable without a database |
| **Interface Projections** | Spring Data projections fetch only needed columns via JOIN — no ORM overhead, no N+1 |
| **JWT + Refresh Token Rotation** | Stateless auth with secure token lifecycle — industry-standard for SPAs and mobile apps |


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
| **Access Token** | Client (header) | 15 min by default (`production` profile: 900000000 ms ≈ 10.4 days) | Stateless API authentication |
| **Refresh Token** | Database | 7 days by default (`production` profile: 30 days) | Obtain new access token without re-login |

- **Rotation**: Each refresh generates a new refresh token and invalidates the old one
- **Revocation**: Logout revokes all refresh tokens for the user
- **Claims**: `sub` = userId (UUID), `email`, `roles` (list of role codes), `organizationId` (when the user belongs to an organization)
- **Brute-force protection**: failed logins are counted in Redis (`login:attempts:{id}`); 5 failures set `login:lock:{id}` for 30 minutes

### Public Endpoints (No Auth Required)

```
POST   /api/v1/auth/register | login | refresh | resend-verification | forgot-password | reset-password
GET    /api/v1/auth/verify-email
GET    /api/v1/pets/**
       /api/v1/rescue-cases/**, /api/v1/organizations/map/**
       /chat-ws/** (JWT checked by the handshake interceptor), /ws/**
GET    /swagger-ui.html, /api-docs
GET    /actuator/**
```

## Conventions

- **URI pattern**: `/api/v1/**` — all responses wrapped in `ApiResponse<T>`
- **`@Transactional`**: Only on domain services (plus read-only query adapters) — never on use cases or controllers. Plain `@Transactional` uses the JPA manager; MongoDB services use `@Transactional(transactionManager = "mongoTransactionManager")` (see `MongoConfig`)
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


## Transaction Boundaries

| Layer | `@Transactional`? | Why |
|---|---|---|
| Domain services (`domain/service/*DomainService`) | **Yes.** Writes use the default, reads use `readOnly = true` | Business rules and state changes live here |
| Query adapters (`infrastructure/persistence/adapter/*QueryAdapter`) | **Yes**, class-level `readOnly = true` | Mapping may touch lazy relations |
| `CustomUserDetailsService.loadUserByUsername()` | **Yes**, `readOnly = true` | Reads `user.getRoles()`, which is lazy |
| Use cases (`application/usecase/*UseCase`) | **No** | Thin orchestrators; the domain service opens the transaction |
| Command adapters (`*RepositoryAdapter`) | **No** | Translation only; they join the domain service's transaction |
| Controllers | **No** | They call ports, never manage transactions |

```
Command: Controller → Port → UseCase → DomainService (@Transactional) → Adapter → JPA
Query:   Controller → Port → UseCase → QueryAdapter (@Transactional readOnly) → JPA
```

`failed to lazily initialize a collection ... no Session` means a lazy collection was read outside a transaction: add `@Transactional(readOnly = true)` to the method that reads it, or fetch it eagerly in the query.

MongoDB services use `@Transactional(transactionManager = "mongoTransactionManager")` (see `MongoConfig`). There is no JPA session inside a Mongo transaction, so prefer `existsById` over loading entities with lazy relations there.
