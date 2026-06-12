# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Repository structure

Two sub-projects that are always developed together:

- `karate-ops-fe/` — React 18 + TypeScript + Vite frontend (SPA)
- `karate-tournament-backend/` — Spring Boot 3.5 + Java 17 + Maven backend

---

## Frontend (`karate-ops-fe`)

### Commands

```powershell
cd karate-ops-fe
npm install          # install deps
npm run dev          # dev server on :5173 (proxies /api, /ws to :8080)
npm run build        # production build into dist/
npm start            # serve dist/ with Express on :4173
npm run check        # TypeScript check (tsc --noEmit) + server.js syntax check
```

No test runner is configured; `npm run check` is the only verification step.

### Architecture

**Routing** is manual — no React Router. `App.tsx` reads `window.location.pathname` and renders the appropriate page component. All top-level pages live in `App.tsx` (one large file), except `ClubManagementPage.tsx` which is imported separately.

**Route map:**
| Path | Component |
|------|-----------|
| `/` | `PublicHomePage` |
| `/login` | `LoginPage` |
| `/register` | `RegisterPage` |
| `/app` | `HomePage` (hub) |
| `/member` | `MemberPortalPage` |
| `/clubs[/:id]` | `ClubManagementPage` |
| `/tournaments` | `TournamentManagementPage` |
| `/dashboard/tournaments/:id` | `TournamentDashboardPage` |
| `/control?tatamiId=&tournamentId=` | `ControlPage` |
| `/display?tatamiId=&tournamentId=` | `DisplayPage` |
| `/judge?tatamiId=&tournamentId=` | `JudgePage` |
| `/overlay?...` | `OverlayPage` |

**Tatami views** (`/control`, `/display`, `/judge`, `/overlay`) share state via the `useScoreboard` hook (`src/useScoreboard.ts`). The hook does an initial REST fetch of the current match, then subscribes to the STOMP topic `/topic/tatamis/{tatamiId}` for live updates. Actions from the control desk POST to `/api/matches/{id}/events` and `/api/matches/{id}/result`.

If no `tatamiId` query param is present, `useManualTatami` (local-only state) is used — the "free tatami" demo mode accessible without auth.

**API client** (`src/apiClient.ts`): All requests go through `apiGet/apiPost/apiPatch/apiPut/apiDelete`. JWT token stored in `sessionStorage` under `karate-ops.authToken`. On a 401 the token is cleared and user is redirected to `/login`. The backend wraps every response in an `ApiEnvelope<T>`; the client unwraps `.data` on success.

`VITE_API_BASE_URL` env var sets the API origin; defaults to same-origin (proxied in dev via `vite.config.ts`).

**Club features** are split into `src/features/clubs/` with `clubApi.ts`, `clubUtils.ts`, `clubConstants.ts` and tab components (`MembersTab`, `AttendanceTab`, `FeesTab`, `AnnouncementsTab`, `RequestsTab`). The club workspace sidebar is grouped via `CLUB_TAB_GROUPS` in `clubConstants.ts`. `RequestsTab` handles both leave requests and tournament join requests; `LeaveRequestForm` is shared with the member portal. Everything else lives in `src/`.

**Role system**: `AuthUserResponse.roles` is a string array (`GLOBAL_ADMIN`, `CLUB_MANAGER`, `MEMBER`). `GLOBAL_ADMIN` can use a "View as" selector to emulate other roles in the UI.

**Animations**: GSAP + `@gsap/react` for the public landing page; Framer Motion for page transitions and auth forms.

---

## Backend (`karate-tournament-backend`)

### Commands

Set `JAVA_HOME` to a JDK 17+ runtime (Android Studio JBR 21 is available on this machine):

```powershell
$env:JAVA_HOME = 'C:\Users\hoang\AppData\Local\Programs\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

```powershell
cd karate-tournament-backend
docker compose up -d postgres          # start PostgreSQL 16
mvn spring-boot:run                    # run app on :8080
mvn test                               # run all tests
mvn test -Dtest=FlywayMigrationIT      # run a single test class
```

Default DB: `jdbc:postgresql://localhost:5432/karate_tournament`, user `postgres`, password `123456`.
Seed admin credentials: `admin@karate-ops.local` / `Admin@123456`.

### Architecture

**API envelope**: `ApiResponseBodyAdvice` wraps every controller response in `ApiResponse<T>` (`success`, `status`, `code`, `message`, `data`, `at`, `path`). `ApiExceptionHandler` produces the same shape for errors. The frontend `apiClient.ts` detects and unwraps `data` automatically.

**Exception hierarchy**: `ApiException` (abstract, carries a `code()`) → `ResourceNotFoundException`, `BadRequestException`, `BusinessConflictException`, `ForbiddenException`, `UnauthorizedException`. Throw the appropriate subclass from services; the handler maps them to HTTP status codes.

**Auth**: `JwtAuthenticationFilter` validates `Bearer` tokens and populates `SecurityContextHolder`. Services receive a `CurrentActorProvider` interface (implemented by `JwtCurrentActorProvider`) to check permissions — this decouples domain logic from Spring Security.

**Realtime**: After any match state mutation, call `RealtimePublisher.publishMatch(MatchResponse)`. It pushes the full match response to `/topic/tatamis/{tatamiId}` and a dashboard snapshot to `/topic/tournaments/{tournamentId}/dashboard`. WebSocket endpoint: `/ws` (STOMP).

**Service layer**: All domain logic is behind interfaces in `service/`; implementations in `service/impl/`. Repositories extend Spring Data JPA.

**Concurrency**: Request decision flows (leave requests, tournament join requests, account requests, participant status) must lock the row first via the repository `findWithLockById(...)` methods (`@Lock(PESSIMISTIC_WRITE)`), then re-check the PENDING status inside the `@Transactional` method and throw `BusinessConflictException` if already decided. Duplicate-creation races are backstopped by partial unique indexes plus `saveAndFlush` with `DataIntegrityViolationException` translation.

**Database migrations**: Flyway manages schema in `src/main/resources/db/migration/`. Naming convention: `V{n}__{description}.sql`. Never edit applied migrations; always add a new version.

**Club data model**: A `Person` → `OrganizationMember` must exist before an athlete can join a `ClubRoster`. Tournament entries validate athletes against the delegation organization. The chain Person → OrganizationMember → Athlete/ClubRoster → TournamentParticipant → Entry is the core domain invariant.

**Fee system**: `ClubFeeRole` → `ClubFeeItem` with per-role amounts (`ClubFeeItemRoleAmount`) → per-member role assignment (`OrganizationMemberFeeRole`) → fee assignment records. `ClubTrainingScheduleJob` auto-generates attendance sessions from `ClubTrainingSchedule` records on their scheduled days.

**CORS**: Configured via `APP_CORS_ALLOWED_ORIGINS`; defaults include `:5173`, `:5174`, `:4173`.

---

## Knowledge graph (graphify)

`graphify-out/` holds a persistent knowledge graph of this repo (`graph.json`, `graph.html`, `GRAPH_REPORT.md`).

- **Before answering questions about architecture, file relationships, or "what connects to what"**: query the existing graph first (`/graphify query "<question>"`) instead of re-reading the whole codebase.
- **After completing a task that added, removed, or modified source files**: run `/graphify . --update` to re-sync the graph. Code-only changes use free AST extraction (no LLM tokens); only doc/config changes trigger semantic re-extraction.
- Do not edit files in `graphify-out/` by hand; they are generated.
