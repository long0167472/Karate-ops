---
name: context-index
description: Master navigation index — every query about KarateOps starts here
type: index
version: "2.0"
last_updated: "2026-06-11"
---

# KarateOps Context Index

> **Every query starts here.** Read the relevant section, then follow the file links. Do not read source code before reading the context file for that area.

---

## LANE SEPARATION (read this first)

Two systems hold knowledge. Do not mix them.

| Question type | Go to |
|--------------|-------|
| What calls what? Where is X defined? Which files import Y? | `/graphify query "…"` |
| What does X *mean*? What rule applies? What is the flow? | Context files below |

**Graph → structure. Context → meaning.**

Stale check — before acting on any context file, verify no source changes since `last_updated`:
```powershell
git log --oneline --since="2026-06-11" -- <source_file_path>
```
Non-empty output: re-read source, mark file `<!-- STALE: re-verify -->`, open a context update task.

---

## DIRECTORY

### Architecture (system shape, no business rules)

| File | One-line description |
|------|---------------------|
| [architecture/overview.md](../architecture/overview.md) | Full component diagram, 4 core data flows, god nodes (AppUser/Organization/Person), RBAC table, tech stack |
| [architecture/frontend.md](../architecture/frontend.md) | React SPA routing table, `apiClient.ts` JWT flow, tatami views, `useScoreboard` hook, club feature modules |
| [architecture/backend.md](../architecture/backend.md) | Spring Boot layers, `ApiResponse<T>` envelope, exception hierarchy, domain invariant chain, STOMP config |

### Features (business flows, actors, edge cases)

| File | One-line description |
|------|---------------------|
| [features/auth-account.md](../features/auth-account.md) | Login + JWT issuance, account-request onboarding (3-step), `/me`, user enumeration protection |
| [features/club-roster-membership.md](../features/club-roster-membership.md) | Person→Member→Athlete→ClubRoster invariant chain, CRUD flows, multi-club roster |
| [features/training-attendance.md](../features/training-attendance.md) | Auto-session generation (job + manual), attendance marking, leave request workflow |
| [features/fee-system.md](../features/fee-system.md) | Fee item lifecycle, billing cycle defaulting (BE owns), due date computation, assignment batch |
| [features/tournament-operations.md](../features/tournament-operations.md) | Tournament lifecycle, delegation APPROVED gate, category setup, entry validation chain |
| [features/draw-brackets.md](../features/draw-brackets.md) | `nextPowerOfTwo` bracket sizing, BYE auto-advance, repechage bronze, ROUND_ROBIN/POOL unimplemented |
| [features/tatami-match-scoring.md](../features/tatami-match-scoring.md) | Scoring events, timer, penalties, `applyKumiteSuggestion`, result confirmation, STOMP broadcast |
| [features/member-self-service.md](../features/member-self-service.md) | `/me` endpoints — club profile, fee summary, attendance history, leave request submission |

### API Reference (exact endpoints, request/response shapes)

| File | One-line description |
|------|---------------------|
| [api/club-api.md](../api/club-api.md) | Organizations, members, roster, attendance sessions/records, fees/finance — 40+ endpoints |
| [api/tournament-api.md](../api/tournament-api.md) | Tournaments, delegations, categories, entries, tatamis, draw endpoint |
| [api/match-api.md](../api/match-api.md) | `POST /events` ScoreEventType dispatch table, `POST /result` ConfirmResultRequest, WinType values |

### Services (algorithms, internal logic)

| File | One-line description |
|------|---------------------|
| [services/auth-module.md](../services/auth-module.md) | JWT payload structure, HMAC-SHA256 signing, RBAC scope enforcement, admin seeding UUIDs |
| [services/business-fee-service.md](../services/business-fee-service.md) | `resolveAmount()` minimum-role algorithm, `defaultDueDate()` clamp [1,28], batch assignment logic |

### Data Models (entity schemas, field types, enums)

| File | One-line description |
|------|---------------------|
| [data-models/data-model-overview.md](../data-models/data-model-overview.md) | All 34 entities by domain group, god nodes, soft-delete pattern, **every enum with values** |
| [data-models/auth-domain-entities.md](../data-models/auth-domain-entities.md) | `AppUser`, `Role`, `UserRoleAssignment`, `AccountRequest` — fields, types, fixed admin UUIDs |
| [data-models/person-club-entities.md](../data-models/person-club-entities.md) | `Organization`, `Person`, `OrganizationMember`, `Athlete`, `ClubRoster` — PII fields marked |
| [data-models/attendance-entities.md](../data-models/attendance-entities.md) | `AttendanceSession`, `AttendanceRecord`, `LeaveRequest`, `ClubTrainingSchedule` — unique constraints |
| [data-models/fee-entities.md](../data-models/fee-entities.md) | `ClubFeeRole`, `ClubFeeItem`, `ClubFeeItemRoleAmount`, `MemberFeeAssignment`, `TuitionOverride` |
| [data-models/tournament-entities.md](../data-models/tournament-entities.md) | `Tournament`, `TournamentParticipant`, `Category`, `Entry`, `Bracket`, `CategoryResult` |
| [data-models/match-entities.md](../data-models/match-entities.md) | `Match`, `KumiteMatchState` (@MapsId), `MatchParticipant`, `KataVote`, `MatchScoreEvent`, `Tatami` |

### Rules & Governance (what must be true)

| File | One-line description |
|------|---------------------|
| [rules/coding-standards.md](../rules/coding-standards.md) | 10 rules: BE owns logic (Rule 1), never edit migrations (Rule 2), soft-delete only (Rule 6), realtime publish required (Rule 8) |
| [governance/api-standards.md](../governance/api-standards.md) | `ApiResponse<T>` envelope structure, exception→HTTP mapping, URI style, WebSocket topic names |
| [client-rules/client-specific-rules.md](../client-rules/client-specific-rules.md) | AUTH/SVC/ENV/PII rules, 10 forbidden patterns, org-scope isolation invariant |

### Known Issues (bugs, fragile areas)

| File | One-line description |
|------|---------------------|
| [known-issues/patterns.md](../known-issues/patterns.md) | 8 patterns: state machine no guard (P2), fee batch no idempotency (P3), Person/AppUser duality (P4), V4 seed in prod (P6) |
| [known-issues/billing-cycle-leak.md](../known-issues/billing-cycle-leak.md) | BillingCycle bug — FE was duplicating BE logic; fixed 2026-06-11; Rule 1 extracted |

### Indexes

| File | One-line description |
|------|---------------------|
| [indexes/_expected-inventory.md](../indexes/_expected-inventory.md) | Phase 1 inventory: 20 features, 34 entities, 7 issues, 7 open questions |
| [indexes/coverage-audit.md](../indexes/coverage-audit.md) | Phase 4 audit: coverage gaps, self-critique corrections, NEEDS HUMAN INPUT list |

---

## FEATURE MAP

For each feature: which services it relies on, which rules apply, known risks.

### Auth & Account Onboarding
- **Services**: `services/auth-module.md` (JWT, RBAC, seeding)
- **Data models**: `data-models/auth-domain-entities.md`
- **Rules**: client-rules AUTH-1 through AUTH-6, coding-standards Rule 1
- **Known risks**: temporary password `"123456"` in response body (Pattern 7); register endpoint disabled (AUTH-6)

### Club Roster & Membership
- **Services**: `services/auth-module.md` (permission checks)
- **Data models**: `data-models/person-club-entities.md`
- **Rules**: coding-standards Rule 7 (invariant chain), client-rules PII-1 through PII-4
- **Known risks**: Pattern 4 (Person/AppUser duality — `Athlete.person` ≠ `AppUser`)

### Training Attendance
- **Services**: None (thin service layer, job-driven)
- **Data models**: `data-models/attendance-entities.md`
- **Rules**: coding-standards Rule 6 (soft-delete), client-rules SVC-4
- **Known risks**: Pattern 5 (resolved — idempotency present); leave approval does NOT auto-set record to EXCUSED

### Fee System
- **Services**: `services/business-fee-service.md` (`resolveAmount`, `defaultDueDate`, batch apply)
- **Data models**: `data-models/fee-entities.md`
- **Rules**: coding-standards Rule 1 (BE owns logic), client-rules SVC-1
- **Known risks**: Pattern 1 (BillingCycle leak — resolved); Pattern 3 (batch apply no idempotency key)

### Tournament Operations
- **Services**: `services/auth-module.md` (permission check on create)
- **Data models**: `data-models/tournament-entities.md`
- **Rules**: coding-standards Rule 7 (domain chain), client-rules SVC-2
- **Known risks**: entry weight `OUT_OF_CLASS` does NOT block registration (Pattern 8)

### Draw & Brackets
- **Services**: None (`DrawServiceImpl` is the algorithm)
- **Data models**: `data-models/tournament-entities.md`, `data-models/match-entities.md`
- **Rules**: coding-standards Rule 8 (no realtime publish needed here — draw is not live)
- **Known risks**: ROUND_ROBIN and POOL bracket types are unimplemented (enum exists, no draw logic)

### Live Match Scoring
- **Services**: `services/auth-module.md` (TATAMI_OPERATOR permission); `governance/api-standards.md` (STOMP topic names)
- **Data models**: `data-models/match-entities.md`
- **Rules**: coding-standards Rule 8 (realtime publish required after EVERY mutation)
- **Known risks**: Pattern 2 (no transition guard on STATUS_CHANGE); timer desync if `timerStartedAt` lost

### Member Self-Service
- **Services**: `services/auth-module.md` (MEMBER role always added at login)
- **Data models**: `data-models/person-club-entities.md`, `data-models/fee-entities.md`, `data-models/attendance-entities.md`
- **Rules**: client-rules AUTH-3 (MEMBER role implicit)
- **Known risks**: null `primaryOrganizationId` causes `ResourceNotFoundException` on all `/me` calls

---

## SERVICE MAP

For each service: which features depend on it, what breaks if it changes.

### `services/auth-module.md` (JwtServiceImpl + AuthServiceImpl + PermissionServiceImpl)
Depended on by: **every feature** — all service methods call `CurrentActorProvider` for the actor; all controllers require a valid JWT.
- Change JWT payload structure → all FE session storage + `/me` response breaks
- Change role loading → RBAC scope checks across all controllers break
- Change admin seed UUIDs → data migrations and tests that reference fixed UUIDs break
- Change HMAC secret rotation → all existing tokens immediately invalidated

### `services/business-fee-service.md` (ClubFeeServiceImpl)
Depended on by: **Fee System** (direct), **Member Self-Service** (`/me/fees`).
- Change `resolveAmount()` → per-member fee amounts change; `MemberFeeAssignment` records created with different amounts
- Change `defaultDueDate()` → all batch-applied assignments get different due dates
- Change `inferFeeKind()` / billing cycle defaulting → FE must not re-introduce defaults (Rule 1 guard)
- Change `ensureDefaultTuitionItem()` → organizations without the default item stop loading the finance overview

---

## QUICK LOOKUP

### "When changing X, what do I need to read?"

| Changing | Read first | Then check |
|---------|-----------|------------|
| `AuthServiceImpl` / `JwtServiceImpl` | `services/auth-module.md` | `client-rules/client-specific-rules.md` AUTH rules, `known-issues/patterns.md` |
| `ClubFeeServiceImpl` | `services/business-fee-service.md` | `features/fee-system.md`, `rules/coding-standards.md` Rule 1, `known-issues/patterns.md` Pattern 1+3 |
| `MatchServiceImpl` | `features/tatami-match-scoring.md` | `api/match-api.md`, `data-models/match-entities.md`, `known-issues/patterns.md` Pattern 2 |
| `DrawServiceImpl` | `features/draw-brackets.md` | `data-models/tournament-entities.md`, `data-models/match-entities.md` |
| `ClubTrainingScheduleJob` | `features/training-attendance.md` | `data-models/attendance-entities.md` |
| `AccountProvisioningServiceImpl` | `features/auth-account.md` | `data-models/auth-domain-entities.md`, `data-models/person-club-entities.md` |
| Any Flyway migration file | `rules/coding-standards.md` Rule 2 | `known-issues/patterns.md` Pattern 6 (V4 risk) |
| `useScoreboard.ts` / tatami views | `architecture/frontend.md` | `features/tatami-match-scoring.md`, `governance/api-standards.md` (STOMP topics) |
| `apiClient.ts` | `architecture/frontend.md` | `governance/api-standards.md`, `services/auth-module.md` (JWT format) |
| Any entity class | `data-models/data-model-overview.md` | Domain-specific entity file, then check if soft-delete is handled |

### "For task type X, what do I read?"

| Task | Read |
|------|------|
| Add a new REST endpoint | `governance/api-standards.md` (envelope, error codes), `rules/coding-standards.md` (exception types), `client-rules/client-specific-rules.md` (auth rules) |
| Add a new entity / DB table | `data-models/data-model-overview.md` (soft-delete, BaseEntity), `rules/coding-standards.md` Rule 2 (migrations), domain entity file |
| Fix a scoring/match bug | `features/tatami-match-scoring.md`, `data-models/match-entities.md`, `known-issues/patterns.md` Pattern 2 |
| Add fee calculation logic | `services/business-fee-service.md`, `rules/coding-standards.md` Rule 1, `known-issues/patterns.md` Pattern 1+3 |
| Change tournament draw | `features/draw-brackets.md`, `data-models/tournament-entities.md` |
| Add a new FE page/route | `architecture/frontend.md` (routing table), `governance/api-standards.md` (how FE calls API) |
| Debug a 401/403 | `services/auth-module.md` (JWT verify, expiry), `client-rules/client-specific-rules.md` AUTH rules |
| Debug a WebSocket issue | `features/tatami-match-scoring.md` (STOMP section), `governance/api-standards.md` (WebSocket topics), `architecture/overview.md` |
| Handle a new org-scoped feature | `client-rules/client-specific-rules.md` AUTH-2 (org scope), `data-models/person-club-entities.md` |
| Write a new Flyway migration | `rules/coding-standards.md` Rule 2, `known-issues/patterns.md` Pattern 6 |

---

## NEW DEVELOPER START HERE

Read these 5 files in order. Each one builds on the previous.

**1. [`architecture/overview.md`](../architecture/overview.md)**
*Why first*: Gives you the full picture — what the system does, how FE/BE/DB connect, the 4 core data flows, and which entities are gods. Without this, every other file is a puzzle piece with no picture on the box.

**2. [`client-rules/client-specific-rules.md`](../client-rules/client-specific-rules.md)**
*Why second*: Lists what you must never do. The 10 forbidden patterns and AUTH/SVC/ENV/PII rules are the guardrails. Read before writing any code so you don't accidentally recreate a past bug.

**3. [`data-models/data-model-overview.md`](../data-models/data-model-overview.md)**
*Why third*: Shows you the 34 entities, their domain groupings, the domain invariant chain, and every enum value. This is your vocabulary — every service, controller, and test speaks in these terms.

**4. [`features/auth-account.md`](../features/auth-account.md) + [`services/auth-module.md`](../services/auth-module.md)**
*Why fourth*: Auth gates every other feature. You need to know how JWTs are built, how roles are loaded, and what `CurrentActorProvider` is before you can reason about permissions anywhere in the codebase.

**5. [`known-issues/patterns.md`](../known-issues/patterns.md)**
*Why fifth*: Knowing the 8 confirmed bug patterns before you start means you won't accidentally walk into them. Pattern 2 (no match state guard), Pattern 4 (Person≠AppUser), and Pattern 6 (V4 prod migration) have all burned time in the past.

---

## EXAMPLE QUERIES

### Query 1: "I need to add a new scoring event type to the match system"

**Start here**: `features/tatami-match-scoring.md` → ScoreEventType dispatch table  
**Then read**: `api/match-api.md` → MatchEventRequest fields  
**Then read**: `data-models/match-entities.md` → MatchScoreEvent fields (what gets persisted)  
**Rules to apply**: `rules/coding-standards.md` Rule 8 — `realtimePublisher.publishMatch()` must be called after every mutation  
**Watch out for**: `known-issues/patterns.md` Pattern 2 — if the new event triggers a state change, there is no transition guard; you must add the guard yourself  

**Expected answer**: Add a new `ScoreEventType` enum value → handle it in `MatchServiceImpl.applyEvent()` switch → persist a `MatchScoreEvent` with the event data → call `realtimePublisher.publishMatch()` → add the event shape to `MatchEventRequest` record.

---

### Query 2: "Why is a member's monthly fee amount wrong after I changed the fee role?"

**Start here**: `services/business-fee-service.md` → `resolveAmount()` algorithm  
**Key rule**: resolveAmount selects the **MINIMUM** `ClubFeeItemRoleAmount` across all of the member's fee roles. If a member has multiple roles, the cheapest one wins.  
**Then check**: `data-models/fee-entities.md` → `OrganizationMemberFeeRole` (member↔role link) and `ClubFeeItemRoleAmount` (role↔item price)  
**Watch out for**: `known-issues/patterns.md` Pattern 3 — changing a fee role does NOT retroactively update existing `MemberFeeAssignment` records. Old assignments keep their `amountDue`. You must re-run `applyFeeItem()` to create new assignments; existing ones stay.  
**Rules**: `rules/coding-standards.md` Rule 1 — do not recalculate fee amounts on the FE.

**Expected answer**: The existing `MemberFeeAssignment.amountDue` was computed at apply-time and is not updated when roles change. Re-apply the fee item to regenerate assignments. Check if the member has multiple fee roles — only the minimum-priced role's amount is used.

---

### Query 3: "How do I add a new page to the frontend that requires CLUB_MANAGER role?"

**Start here**: `architecture/frontend.md` → routing table (manual routing via `window.location.pathname` in `App.tsx`)  
**Auth check**: `services/auth-module.md` → JWT payload — the `roles` array is in the token, loaded by `useScoreboard`/`apiClient` on mount  
**API rules**: `governance/api-standards.md` → all requests go through `apiGet/apiPost` from `apiClient.ts`; JWT is in `sessionStorage["karate-ops.authToken"]`; 401 clears token and redirects to `/login`  
**Rules**: `client-rules/client-specific-rules.md` AUTH-2 — CLUB_MANAGER is org-scoped; must pass the correct `organizationId` in requests  
**Implementation steps**:
1. Add a new route case in `App.tsx` `window.location.pathname` switch
2. Create the page component — check `AuthUserResponse.roles` (available from `GET /api/auth/me`) for `"CLUB_MANAGER"`
3. For API calls, use `apiGet/apiPost` — no manual token handling needed
4. For BE endpoints you're adding, use `permissionService.requireRosterManage(orgId)` (or equivalent) in the service layer — do not put auth checks in the controller

**Expected answer**: Add a route to `App.tsx`, check `roles.includes("CLUB_MANAGER")` in the component, use `apiClient.ts` helpers for requests, enforce org-scope permission in the BE service layer.

---

## MAINTENANCE

- **After modifying any source file referenced in context**: run `git log --oneline --since="2026-06-11" -- <file>` and update the relevant context file's `last_updated` + content.
- **After adding/removing source files**: run `/graphify . --update` to resync the knowledge graph.
- **After a context file is reviewed by a human**: remove any `[LOW CONFIDENCE]` or `[NEEDS HUMAN INPUT]` markers that were resolved.
- Open questions awaiting human input are tracked in [`indexes/coverage-audit.md`](../indexes/coverage-audit.md).
