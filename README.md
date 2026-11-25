# Pet Rescue API

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL%20%2B%20PostGIS-16-336791)
![MongoDB](https://img.shields.io/badge/MongoDB-7-47A248)
![License](https://img.shields.io/badge/license-educational-lightgrey)

The backend of **PawHome**, a platform that connects people who find animals in trouble, the volunteers who rescue them, the centres that care for them, and the families who adopt them. Built with Spring Boot 3.5 and Java 21, following Clean Architecture and CQRS.

- **Frontend:** [paw-home-ui](https://github.com/Nartt11/paw-home-ui) (Next.js)
- **API reference:** Swagger UI at `/swagger-ui.html` on a running server
- **Demo data and scenarios:** [scripts/demo-data](scripts/demo-data/README.md)

## Contents

- [Overview](#overview)
- [Features](#features)
- [Demo](#demo)
- [Architecture](#architecture)
- [Tech stack](#tech-stack)
- [Getting started](#getting-started)
- [Configuration](#configuration)
- [API](#api)
- [Testing](#testing)
- [Deployment](#deployment)
- [Project structure](#project-structure)
- [Documentation](#documentation)
- [License](#license)

## Overview

| Who | What they do |
|---|---|
| **Anyone** | Reports an animal in trouble with a photo and a pin on the map, browses pets waiting for a home, reads the community feed |
| **Users** | Take a case nearby and post progress, report a lost pet and get matched with found ones, apply to adopt, answer check-ins after adoption, post and chat |
| **Centre members** (owner, staff, vet) | Pick up cases, care for their pets, review applications ranked by how well the home fits, follow up after adoption, reclaim a pet when needed |
| **Admins** | Approve centres, settle disputed rescues and flagged cases, configure the AI, manage banners, users and roles |

Roles: every account has one **system role** (`ADMIN`, `USER` or `MEMBER`). A `MEMBER` also has a role inside one centre (`OWNER`, `STAFF` or `VET`), and centre roles are checked only on centre routes.

## Features

**Rescue**
- Report a case with photos, a map pin and the place resolved against the 34-province address catalogue; cases near a point or inside the map view (PostGIS)
- Take a case, post progress updates with photos, send a completion report that the reporter confirms or disputes; disputes go to admins
- Lost and found: a lost-pet case is matched with found cases nearby by species, colour, marks, distance and time
- Community flags (false report, already handled, duplicate) and an AI triage of every new case: urgency, whether it needs a rescue, a condition summary, missing details

**Adoption and matching**
- Centres register pets with photos, medical records and traits (temperament, energy, time alone, cost, care level)
- Adopters fill in an adopter profile once; each application is scored against the pet (level, score, certainty, reasons, hard warnings such as allergy or building rules) and ranked for the centre
- Approve, hand-over, cancel and reject flows; a centre can reclaim a pet, which is logged and costs the adopter reputation

**After adoption**
- Check-ins at day 2, week 2, month 3 and month 6 by e-mail link or in the app
- Worrying answers raise red or yellow alerts for the centre; requests for help or to give the pet back are tracked to an outcome

**Community and chat**
- Posts, comments and likes in MongoDB, counters updated through RabbitMQ and Redis
- AI reads each post (Vietnamese written with or without accents, abbreviations, slang): category, species and place, which the author confirms
- Real-time chat over WebSocket (`/chat-ws`), friends, notifications and push tokens

**Platform**
- JWT access and refresh tokens with rotation, e-mail verification, login lockout after 5 failures
- Admin dashboard with figures cached in Redis, banners, roles and tags
- Media on Cloudinary; AI through Claude, Gemini or OpenRouter, chosen at runtime by admins

## Demo

The screenshots come from the [paw-home-ui](https://github.com/Nartt11/paw-home-ui) frontend running on this API with the [demo data](scripts/demo-data/README.md) loaded. Every account below uses the password `Demo@12345`, and each one walks through the scenarios of its role without switching accounts. The demo data README lists every scenario step by step.

| Account | Role | Try |
|---|---|---|
| `demo.user1@petrescue.demo` | User | Confirm a rescue, follow a case you took, your pets, a lost cat with a possible match, an application and its check-ins, chats |
| `demo.owner1@petrescue.demo` | Owner of Trạm cứu hộ Mái Ấm Sài Gòn | Applications ranked by fit, hand-over, alerts after adoption, a reclaim, the rescue queue, pet traits |
| `demo.admin@petrescue.demo` | Admin | Dashboard, centres waiting for approval, disputed and flagged cases, AI settings, banners |

### Public pages

<table>
  <tr>
    <td width="50%"><img src="docs/assets/screenshots/01-home.jpg" alt="Home page with live figures"><br><sub><b>Home.</b> Live figures and the banner admins set.</sub></td>
    <td width="50%"><img src="docs/assets/screenshots/02-rescue-map.jpg" alt="Rescue map with clustered cases"><br><sub><b>Rescue map.</b> Clustered cases and centres; the list follows the map view.</sub></td>
  </tr>
  <tr>
    <td><img src="docs/assets/screenshots/03-adoption-list.jpg" alt="Pets waiting for adoption"><br><sub><b>Adopt.</b> Pets waiting for a home, filtered by species, health and centre.</sub></td>
    <td><img src="docs/assets/screenshots/04-community.jpg" alt="Community feed"><br><sub><b>Community.</b> Posts, some linked to a rescue case that still needs help.</sub></td>
  </tr>
</table>

### User (`demo.user1`)

<table>
  <tr>
    <td width="50%"><img src="docs/assets/screenshots/11-user-my-pets.jpg" alt="My pets"><br><sub><b>My pets.</b> Adopted through PawHome, rescued and kept, or registered by the owner.</sub></td>
    <td width="50%"><img src="docs/assets/screenshots/13-user-adoptions.jpg" alt="My applications and check-ins"><br><sub><b>My applications.</b> One waiting for review; for an adopted pet, the check-in schedule and ways to ask for help.</sub></td>
  </tr>
  <tr>
    <td><img src="docs/assets/screenshots/12-user-my-cases.jpg" alt="My cases"><br><sub><b>My cases.</b> Cases you are helping with and cases you reported.</sub></td>
    <td><img src="docs/assets/screenshots/10-user-pet-match.jpg" alt="Pet detail"><br><sub><b>Pet detail.</b> Health, vaccination and the centre caring for the pet.</sub></td>
  </tr>
</table>

### Centre owner (`demo.owner1`)

<table>
  <tr>
    <td width="50%"><img src="docs/assets/screenshots/21-owner-adoptions.jpg" alt="Applications waiting for review"><br><sub><b>Applications.</b> One next step per row, refusing kept in the ⋮ menu, and the reclaim log.</sub></td>
    <td width="50%"><img src="docs/assets/screenshots/22-owner-adoption-match.jpg" alt="Application with its match score"><br><sub><b>Match.</b> How well the home fits the pet, with the reasons and what to prepare.</sub></td>
  </tr>
  <tr>
    <td><img src="docs/assets/screenshots/23-owner-followup-alerts.jpg" alt="Alerts after adoption"><br><sub><b>After adoption.</b> Urgent first: a family thinking of giving the pet back, a worried adopter being helped.</sub></td>
    <td><img src="docs/assets/screenshots/24-owner-rescue-queue.jpg" alt="Rescue queue"><br><sub><b>Rescue queue.</b> Untaken cases in the centre's province, most urgent first, with the AI's reading.</sub></td>
  </tr>
  <tr>
    <td><img src="docs/assets/screenshots/25-owner-pet-traits.jpg" alt="Pet traits"><br><sub><b>Pet traits.</b> What staff observed, used to score applications.</sub></td>
    <td><img src="docs/assets/screenshots/20-owner-dashboard.jpg" alt="Centre dashboard"><br><sub><b>Centre dashboard.</b> The centre's month at a glance.</sub></td>
  </tr>
</table>

### Admin (`demo.admin`)

<table>
  <tr>
    <td width="50%"><img src="docs/assets/screenshots/30-admin-dashboard.jpg" alt="Admin dashboard"><br><sub><b>Dashboard.</b> Centres, pets, open cases and applications, month by month.</sub></td>
    <td width="50%"><img src="docs/assets/screenshots/32-admin-rescue.jpg" alt="Rescue management"><br><sub><b>Rescue.</b> Completion reports to review, a disputed one first, next to the map.</sub></td>
  </tr>
  <tr>
    <td><img src="docs/assets/screenshots/31-admin-centres.jpg" alt="Centre management"><br><sub><b>Centres.</b> Two waiting for approval.</sub></td>
    <td><img src="docs/assets/screenshots/34-admin-ai.jpg" alt="AI settings"><br><sub><b>AI settings.</b> The model that reads posts and how often its suggestions are accepted.</sub></td>
  </tr>
  <tr>
    <td><img src="docs/assets/screenshots/33-admin-posts.jpg" alt="Post moderation"><br><sub><b>Posts.</b> Moderation list with filters.</sub></td>
    <td><img src="docs/assets/screenshots/35-admin-banners.jpg" alt="Banners"><br><sub><b>Banners.</b> What runs on the home, adoption and rescue pages.</sub></td>
  </tr>
</table>

### Mobile

<img src="docs/assets/screenshots/40-mobile-map.jpg" alt="Rescue map on a phone" width="300">

### Run the demo yourself

1. Start the backend as in [Getting started](#getting-started).
2. Load the demo data: see [scripts/demo-data/README.md](scripts/demo-data/README.md). Match scores are filled in by the API itself within 5 minutes.
3. Start the [frontend](https://github.com/Nartt11/paw-home-ui) with `NEXT_PUBLIC_API_URL=http://localhost:8080/api/v1`.

To retake the screenshots, capture at 1440×900 and save them as JPEG at 1280 px wide in `docs/assets/screenshots/`, keeping the file names.

## Architecture

```
┌───────────────────────────────────────────────────────────────┐
│ Presentation   Controllers · GlobalExceptionAdvice · ApiResponse │
├──────────────────────────────┬────────────────────────────────┤
│ Command path                 │ Query path (CQRS)              │
│ CommandPort → UseCase        │ QueryPort → UseCase            │
│   → DomainService            │   → QueryDataPort              │
│     (rules, @Transactional)  │     → QueryAdapter             │
│   → Repository port          │       (projection → DTO,       │
│     → RepositoryAdapter      │        @Transactional readOnly)│
├──────────────────────────────┴────────────────────────────────┤
│ Infrastructure  JPA · MongoDB · Redis · RabbitMQ · WebSocket · AI │
├───────────────────────────────────────────────────────────────┤
│ Domain (pure Java)  Entities · Value objects · Ports · Services │
└───────────────────────────────────────────────────────────────┘
```

- **Writes** go through a domain service, where the business rules and the transaction live.
- **Reads** skip the domain: a query adapter runs a JOIN projection and returns DTOs, so there is no N+1.
- **Controllers** only talk to ports. `DevController` and `AdminController` are the only exceptions.
- **PostgreSQL** holds the core data; **MongoDB** holds posts, comments, likes, chat and AI runs; **Redis** caches and counts; **RabbitMQ** carries social events.

The layers, CQRS rules, transaction boundaries and authentication flow are explained in [docs/architecture.md](docs/architecture.md).

## Tech stack

| Area | Technology |
|---|---|
| Language and framework | Java 21, Spring Boot 3.5.10 (Web, Security, Data JPA, Data MongoDB, AMQP, WebSocket, Mail, AOP, Actuator) |
| Data | PostgreSQL 16 + PostGIS 3.5 (Flyway migrations, Hibernate Spatial), MongoDB 7 replica set, Redis 7 |
| Messaging | RabbitMQ 3.13 (topic exchange `social.exchange` with dead-letter queue) |
| Auth | Spring Security, jjwt 0.12.6 (HMAC-SHA), BCrypt |
| Media | Cloudinary SDK 1.39.0 |
| AI | Anthropic Java SDK 2.34.0, Gemini and OpenRouter over HTTP |
| Mapping and docs | MapStruct 1.6.3, Lombok, springdoc-openapi 2.8.5 |
| Tests | JUnit 5, Testcontainers (PostGIS, MongoDB), H2 |
| Delivery | Docker multi-stage build, Docker Compose, GitHub Actions, GHCR |

## Getting started

### Prerequisites

- JDK 21 (or let the Gradle wrapper fetch the toolchain)
- Docker, for PostgreSQL, MongoDB, Redis and RabbitMQ

### Run locally

```bash
# 1. Configuration: copy the template and fill in what you use (JWT_SECRET at least)
cp .env.example .env

# 2. Datastores: PostgreSQL + PostGIS, Redis, RabbitMQ, MongoDB (replica set rs0)
docker-compose up -d

# 3. The API (Flyway migrates the schema on start)
./gradlew bootRun          # Windows: gradlew.bat bootRun
```

The API listens on `http://localhost:8080`, with Swagger UI at `http://localhost:8080/swagger-ui.html`.

To run everything in containers instead:

```bash
docker-compose --profile docker up -d --build
docker-compose logs -f app
docker-compose down -v     # stop and remove volumes
```

### Load demo data

```bash
psql "$DB_URL" -v ON_ERROR_STOP=1 -f scripts/demo-data/seed-postgres.sql
mongosh "$MONGO_URI" scripts/demo-data/seed-mongo.js
```

The [demo data README](scripts/demo-data/README.md) explains how to run them through Docker without installing `psql` or `mongosh`, how to load them into staging, and how to remove the data again.

## Configuration

Settings come from environment variables; `.env.example` lists them all.

| Variable | Purpose |
|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL |
| `MONGO_URI`, `MONGO_DATABASE` | MongoDB (replica set needed for transactions); database `petrescue` by default |
| `REDIS_HOST`, `REDIS_PORT` | Redis |
| `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD` | RabbitMQ |
| `JWT_SECRET` | Signs access tokens |
| `APP_CORS_ALLOWED_ORIGINS` | Frontend origins allowed to call the API |
| `MAIL_*`, `SPRING_MAIL_*`, `EMAIL_FROM`, `EMAIL_VERIFICATION_URL`, `PASSWORD_RESET_URL` | Verification, password reset and check-in e-mails |
| `CLOUDINARY_*` | Media storage |
| `AI_POST_ANALYSIS_ENABLED`, `AI_POST_ANALYSIS_MODEL`, `GEMINI_API_KEY`, `OPENROUTER_API_KEY` | AI reading of posts and cases; off unless enabled |

| Profile | How to activate | Use |
|---|---|---|
| `free` (default) | nothing | 512 MB container: Hikari 5, Tomcat 30 threads, 1 consumer per queue. Local development and free hosting |
| `scale` | `SPRING_PROFILES_ACTIVE=scale` | 1.5 GB container: Hikari 10, Tomcat 80 threads, up to 2 consumers |
| `dev` | `SPRING_PROFILES_ACTIVE=dev` | Debug and SQL logs, `/api/v1/dev` endpoints |
| `production` | `SPRING_PROFILES_ACTIVE=production` | Managed PostgreSQL, Redis, RabbitMQ and MongoDB over SSL, lazy init, Swagger UI off |

## API

- Base path `/api/v1`. Every response is wrapped in `ApiResponse<T>` (`success`, `status`, `message`, `data`, `timestamp`, `correlationId`), and lists are paged in `PageResponse<T>`.
- Authentication: `POST /auth/login` returns a JWT access token and a refresh token; send `Authorization: Bearer <token>`. `POST /auth/refresh` rotates both. The current user is `GET /users/me`.
- Public without a token: registration and login, `GET` on pets, rescue cases, centres, the post feed and single posts, active banners, the check-in links sent by e-mail, Swagger and Actuator. `SecurityConfig` has the exact list.
- The full list of endpoints, with request and response schemas, is in Swagger UI at `/swagger-ui.html`.

## Testing

```bash
./gradlew test
```

Integration tests start PostGIS and MongoDB with Testcontainers, so Docker must be running. `DemoDataScriptsIntegrationTest` also loads the demo data scripts against the real schema, so a migration that breaks them fails the build.

## Deployment

| Workflow | Trigger | Steps |
|---|---|---|
| `.github/workflows/ci.yml` | Push to `master` or `develop`, pull request to `master` | JDK 21 → `./gradlew build` → test reports |
| `.github/workflows/deploy.yml` | Push to `master` | Build → Docker image → push to GHCR → deploy over SSH with `docker-compose.prod.yml` |

Production runs the API container (512 MB) against managed services: Neon (PostgreSQL), cloud Redis, CloudAMQP and MongoDB Atlas. `deploy.yml` exports only the database, JWT and mail secrets; the Redis, RabbitMQ and MongoDB variables must already be set on the server.

The Dockerfile is multi-stage: `eclipse-temurin:21-jdk-alpine` builds, `eclipse-temurin:21-jre-alpine` runs as a non-root user with a health check.

## Project structure

```
src/main/java/com/uit/petrescueapi/
├── domain/           Entities, value objects, repository ports, domain services (pure Java)
├── application/      Command and query ports, use cases, DTOs, application services
├── infrastructure/   JPA and Mongo persistence, security, Redis, RabbitMQ, WebSocket,
│                     AI, matching, follow-ups, notifications, media, schedulers
└── presentation/     REST controllers, exception advice, response envelopes
src/main/resources/db/migration/   Flyway migrations
scripts/demo-data/                 Demo data and its scenarios
docs/                              Architecture, guides, audits and proposals
```

## Documentation

| Document | Content |
|---|---|
| [docs/architecture.md](docs/architecture.md) | Layers, CQRS, transaction boundaries, authentication, conventions |
| [docs/guides/add-cqrs-query-endpoint.md](docs/guides/add-cqrs-query-endpoint.md) | Adding a query endpoint, step by step |
| [scripts/demo-data/README.md](scripts/demo-data/README.md) | Demo accounts and scenarios, loading and removing demo data |
| [migrate.md](migrate.md) | Moving posts and chat to MongoDB |
| [docs/srs-and-feature-proposals-2026-11-16.md](docs/srs-and-feature-proposals-2026-11-16.md) | Requirements and feature proposals |
| [docs/fe-be-status.md](docs/fe-be-status.md) | Which backend features the frontend uses |
| [docs/performance-audit-2026-11-04.md](docs/performance-audit-2026-11-04.md), [docs/ux-walkthrough-2026-11-11.md](docs/ux-walkthrough-2026-11-11.md) | Performance audit and UX walkthrough |

## License

This project is for educational purposes.
