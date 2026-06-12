---
name: system-architecture-overview
description: Full system architecture — stack, component diagram, data flows, god nodes, RBAC model
type: architecture
version: "1.0"
last_updated: "2026-06-11"
criticality: CRITICAL
metadata:
  owner: tech-lead
  affected_services: [karate-ops-fe, karate-tournament-backend]
  affected_domains: [all]
source_references:
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/config/SecurityConfig.java, line_range: "1-end", note: "JWT filter wiring, public endpoints, CORS" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/config/WebSocketConfig.java, line_range: "1-end", note: "STOMP broker config, /ws endpoint" }
  - { file_path: karate-ops-fe/src/apiClient.ts, line_range: "1-end", note: "FE HTTP client, JWT attach, 401 redirect" }
  - { file_path: karate-ops-fe/src/useScoreboard.ts, line_range: "1-end", note: "FE WebSocket subscription hook" }
knowledge_graph_refs:
  - { community: "auth-security", hub_node: "AppUser" }
  - { community: "club-operations", hub_node: "Organization" }
  - { community: "tournament-execution", hub_node: "Match" }
related_context_files:
  - context/architecture/frontend.md
  - context/architecture/backend.md
  - context/indexes/context-index.md
---

# System Architecture Overview — KarateOps

## System Purpose

KarateOps is an internal management platform for karate clubs and tournament operators in Vietnam. It provides two primary functions:
1. **Club operations** — member management, training attendance, fee collection, daily club administration
2. **Tournament execution** — draw generation, live match scoring via STOMP, tatami management, results and medals

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Frontend | React + TypeScript + Vite SPA | 18 / 5 |
| HTTP client | Custom `apiClient.ts` (fetch-based) | — |
| WebSocket client | STOMP over SockJS | — |
| Animations | GSAP (landing), Framer Motion (auth/transitions) | — |
| Backend | Spring Boot + Java + Maven | 3.5 / 17 |
| Auth | Custom HMAC-SHA256 JWT (no Spring Security JWT libs) | — |
| ORM | Spring Data JPA + Hibernate | — |
| Realtime | STOMP WebSocket (`/ws` endpoint) | — |
| Database | PostgreSQL | 16 |
| Migrations | Flyway | 13 versions applied |
| Build/Run env | Maven Wrapper + Docker Compose (postgres only) | — |

## Component Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│  Browser / SPA (React 18 + TypeScript + Vite)                   │
│                                                                  │
│  Pages: Auth · Club · Tournament · Tatami views · Member portal │
│  State: useState/useEffect per page (no Redux)                  │
│  JWT: stored in sessionStorage["karate-ops.authToken"]          │
│                                                                  │
│  ┌────────────────┐     ┌───────────────────────────────────┐  │
│  │  apiClient.ts  │     │  useScoreboard (hook)             │  │
│  │  REST requests │     │  STOMP /ws subscription           │  │
│  │  JWT attached  │     │  receives live MatchResponse      │  │
│  │  auto-unwrap   │     │  re-renders tatami views          │  │
│  │  .data field   │     └────────────────────────────────── │  │
│  └───────┬────────┘              │                          │  │
└──────────┼───────────────────────┼──────────────────────────┘  │
           │ HTTP REST             │ STOMP WebSocket              │
           │ /api/**               │ /ws                          │
           ▼                       ▼                              │
┌──────────────────────────────────────────────────────────────┐ │
│  Spring Boot 3.5 (:8080 in dev)                              │ │
│                                                              │ │
│  ┌─────────────────────────────────────────────────────┐    │ │
│  │  Security Layer                                     │    │ │
│  │  JwtAuthenticationFilter → SecurityContextHolder   │    │ │
│  │  Public: /api/auth/login, /api/public/**, /ws      │    │ │
│  └─────────────────────┬───────────────────────────────┘    │ │
│                        │                                    │ │
│  ┌─────────────────────▼───────────────────────────────┐    │ │
│  │  REST Controllers (13 classes)                      │    │ │
│  │  @RestControllerAdvice: ApiResponseBodyAdvice       │    │ │
│  │  wraps every response in ApiResponse<T>             │    │ │
│  └─────────────────────┬───────────────────────────────┘    │ │
│                        │                                    │ │
│  ┌─────────────────────▼───────────────────────────────┐    │ │
│  │  Service Layer (16 interfaces / 22 impls)           │    │ │
│  │  PermissionService (RBAC + org scoping)             │    │ │
│  │  @Transactional on all write methods                │    │ │
│  └──────────┬──────────────────────┬───────────────────┘    │ │
│             │                      │                        │ │
│  ┌──────────▼────────┐  ┌──────────▼──────────────────┐    │ │
│  │  Spring Data JPA  │  │  RealtimePublisher          │    │ │
│  │  30+ repositories │  │  publishMatch() after each  │    │ │
│  │  soft-delete      │  │  match mutation             │    │ │
│  │  UUID PKs         │  │  pushes to STOMP topics     │    │ │
│  └──────────┬────────┘  └──────────┬──────────────────┘    │ │
│             │                      │                        │ │
└─────────────┼──────────────────────┼────────────────────────┘ │
              │                      │ STOMP broker                │
              ▼                      ▼ (in-memory SimpleBroker)    │
┌─────────────────────────────────────────────────────────────┐   │
│  PostgreSQL 16 — single instance                            │   │
│  DB: karate_tournament                                      │   │
│  34 tables · UUID PKs · soft delete (deleted_at)           │   │
│  13 Flyway migrations                                       │   │
└─────────────────────────────────────────────────────────────┘   │
```

## Core Data Flows

### 1. Login (Auth flow)
```
FE: POST /api/auth/login { email, password }
→ JwtAuthenticationFilter: no token yet, passes through
→ AuthController.login()
→ AuthServiceImpl.login()
   1. users.findByEmailOrUsername(normalized email)
   2. passwordEncoder.matches(request.password, user.passwordHash)   [BCrypt]
   3. user.lastLoginAt = Instant.now()
   4. Build AuthenticatedPrincipal(userId, orgId, email, displayName, roles)
   5. jwtService.createToken(principal)   [HMAC-SHA256, exp = now + expires-minutes]
→ Returns AuthResponse { accessToken, "Bearer", expiresInSeconds, user }
→ ApiResponseBodyAdvice wraps → ApiResponse<AuthResponse>
FE: stores token in sessionStorage["karate-ops.authToken"]
```

### 2. Live Tatami Match Scoring
```
FE Control Desk: POST /api/matches/{id}/events { type: SCORE_DELTA, side: AKA, points: 1 }
→ JwtAuthenticationFilter: validates Bearer token → sets SecurityContext
→ MatchController.recordEvent()
→ MatchServiceImpl.recordEvent()
   1. Permission: requireTatamiOperate(match.tournament)
   2. Load match + KumiteMatchState
   3. Apply event (update score, penalty flags, timer state)
   4. Save MatchScoreEvent record (audit trail)
   5. Save KumiteMatchState changes
   6. realtimePublisher.publishMatch(matchResponse)
      → messagingTemplate.convertAndSend("/topic/tatamis/{tatamiId}", matchResponse)
      → messagingTemplate.convertAndSend("/topic/tournaments/{tournamentId}/dashboard", overview)
FE Display/Judge/Overlay: receives WebSocket message → re-renders scoreboard
```

### 3. Fee Application (Club Finance)
```
FE: POST /organizations/{orgId}/fee-items/{itemId}/apply
    { applyToAllActive: true, dueDate: null }
→ ClubFeeController
→ ClubFeeServiceImpl.applyFeeItem()
   1. Permission: requireRosterManage(orgId)
   2. Resolve target members (all ACTIVE if applyToAllActive)
   3. For each member: resolveAmount(member, item)
      - ONE_TIME_INCOME: use defaultAmount
      - Others: find minimum ClubFeeItemRoleAmount matching member's fee roles
   4. defaultDueDate(item): dueDay clamped [1,28], default 10, LocalDate.now().withDayOfMonth(dueDay)
   5. Create MemberFeeAssignment per member (skip if already assigned)
   6. status = WAIVED if amount==0, else PENDING
→ Returns list of created assignments
```

### 4. Tournament Draw / Bracket Generation
```
FE: POST /categories/{id}/draw { shuffle: true, enableRepechage: true }
→ DrawServiceImpl.draw()
   1. Load active entries (not WITHDRAWN/DISQUALIFIED)
   2. bracketSize = nextPowerOfTwo(entryCount)
   3. Generate matches: bracketSize/2 per round × roundCount rounds
   4. Create KumiteMatchState for each kumite match
   5. Link winner paths: match[i].winnerNextMatch = nextRound[i/2], side = i%2==0 ? AKA : AO
   6. Create bronze matches (if repechage, roundCount ≥ 2)
   7. Assign entries to first-round slots
   8. Auto-advance byes (single entry in match → status=COMPLETED, winType=BYE)
   9. category.status = "DRAWN"
→ Returns DrawResponse { bracketId, bracketSize, entryCount, matches[] }
```

## RBAC Model

```
SystemRole (JWT claim)       Scope                         What they can do
─────────────────────────    ─────────────────────         ──────────────────────────
GLOBAL_ADMIN                 Global                        Everything
CLUB_MANAGER                 Own organization only         Manage members, fees, attendance, create tournaments
TOURNAMENT_OWNER             Own organization only         Manage tournaments they own
COACH                        Own organization only         Manage attendance, view club
TATAMI_OPERATOR              Any tournament                Operate tatami, record matches
JUDGE                        Any tournament                View tournament (voting via /judge route)
VIEWER                       Global read                   View any tournament
MEMBER                       Self only                     View own fees, attendance, submit leave requests
```

Permission check call chain: `Controller → Service → PermissionServiceImpl → CurrentActorProvider → SecurityContextHolder`

## God Nodes (High Betweenness)

| Entity | Why it's a god node | Risk |
|--------|--------------------|----|
| `AppUser` (table: `users`) | Referenced by 8+ tables: tournament.createdByUser, match events, attendance decisions, account requests, role assignments, org.primaryOrg | Changes to auth model ripple through all domains |
| `Organization` (table: `organizations`) | Every club-feature entity has `organization_id` FK | Org soft-delete cascades logically to all club data |
| `Person` (table: `persons`) | Base identity for Athlete, OrganizationMember | Person without AppUser is valid; code assuming 1:1 is wrong |

## Multi-tenancy

**None** — single-tenant deployment. Data isolation is per-organization via `organization_id` columns on every table. CLUB_MANAGER can only see their own organization's data (enforced in PermissionService). GLOBAL_ADMIN sees all.

## Multi-region / Regional Variants

No multi-region deployment. Vietnam-specific features:
- `CompetitionLevel.PHONG_TRAO` / `NANG_CAO` — Vietnamese competition tiers
- Default fee role labels in Vietnamese (V8 migration): "Học viên thường", "Sinh viên", "VĐV đội tuyển", "Ban cán sự CLB"
- Default timezone: `Asia/Ho_Chi_Minh` in ClubTrainingSchedule

## External Integrations

None currently. No external payment gateway, email provider, or identity provider visible in the codebase. Account notifications (accountApproved, accountRejected) call `AccountNotificationService` but implementation not confirmed (may be no-op or log-only).

## Known Hotspots

1. `MatchServiceImpl.recordEvent()` — most complex method; 12 event types, state machine transitions, WebSocket publish
2. `ClubFeeServiceImpl.applyFeeItem()` — batch operation; no idempotency key; partial failure risk
3. `DrawServiceImpl.draw()` — complex algorithm; single transaction; large bracket (64+ entries) may timeout
4. `ClubTrainingScheduleJob` — runs at 00:05 daily; no visible idempotency guard on session creation
