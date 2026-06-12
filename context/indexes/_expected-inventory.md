# Expected Inventory — KarateOps CE Wiki Bootstrap
**Phase 1 output. Awaiting human confirmation before Phase 2 generation begins.**

---

## Project Info

| Field | Value |
|-------|-------|
| Project name | KarateOps |
| Project type | Brownfield maintenance + Enhancement |
| Architecture style | **modular-monolith** (single Spring Boot app, single PostgreSQL DB) |
| Tech stack | Spring Boot 3.5 + Java 17 + Maven · React 18 + TypeScript + Vite · PostgreSQL 16 · STOMP WebSocket |
| Client/Domain | Internal platform: karate club operations + tournament management (Vietnam market) |
| Repo | Mono-repo: `karate-ops-fe/` (SPA) + `karate-tournament-backend/` (REST + WS) |
| Multi-tenant | **No** — org-scoped isolation (every table has `organization_id`), single deployment |
| Multi-region | **No** — Vietnam-only; `CompetitionLevel` enum has `PHONG_TRAO`/`NANG_CAO` (VN-specific) |

---

## FEATURES

Discovery source: REST controllers + service interfaces + FE routes.

| # | Feature Name | Discovery | Priority | Notes |
|---|-------------|-----------|----------|-------|
| 1 | **Auth — Login & JWT** | `AuthController /api/auth` [FROM CODE] | HIGH | All features gate on this |
| 2 | **Club Member Onboarding (Account Requests)** | `AccountRequestController` [FROM CODE] | HIGH | Two-step: member applies → club manager approves |
| 3 | **Club Roster & Membership CRUD** | `ClubRosterController`, `ClubMemberController` [FROM CODE] | HIGH | Core club data; feeds tournament entries |
| 4 | **Training Attendance** | `AttendanceController` + `AttendanceLeaveRequestController` [FROM CODE] | HIGH | Daily workflow; real-time session marking |
| 5 | **Club Finance — Fee Management** | `ClubFeeController` [FROM CODE] | HIGH | Role-based fee items → member assignments; most complex rule engine |
| 6 | **Tournament Admin** | `TournamentController` [FROM CODE] | HIGH | Tournament CRUD + delegation approval |
| 7 | **Tournament Categories & Entries** | `CategoryController` + `DrawService` [FROM CODE] | HIGH | Category creation → entry registration → bracket draw |
| 8 | **Live Match Control (Kumite/Kata)** | `MatchController /api/matches/{id}/events` [FROM CODE] | HIGH | Real-time scoring; publishes to WebSocket |
| 9 | **Tatami Setup & Assignment** | `TatamiController` [FROM CODE] | HIGH | Mat management; current-match tracking |
| 10 | **Member Self-Service Portal** | `MemberSelfController /api/me` [FROM CODE] | MEDIUM | Read-only: fees, attendance, leave requests |
| 11 | **Club Training Schedule** | `ClubTrainingScheduleController` + `ClubTrainingScheduleJob` [FROM CODE] | MEDIUM | Recurring schedule → auto-session generation (scheduled job) |
| 12 | **Club Dashboard & Analytics** | `OrganizationDashboardController` [FROM CODE] | MEDIUM | Aggregated health metrics; derived from lower-level data |
| 13 | **Tournament Dashboard** | `DashboardController /api/dashboard/tournaments/{id}` [FROM CODE] | MEDIUM | Medals, tatami live status, category results |
| 14 | **Draw & Brackets** | `DrawService` called from `CategoryController /draw` [FROM CODE] | MEDIUM | Single-elimination, repechage, round-robin generation |
| 15 | **Athlete Management** | `AthleteController /api/athletes` [FROM CODE] | MEDIUM | Person → Athlete linking; tournament eligibility |
| 16 | **Person (Identity) Management** | `PersonController /api/persons` [FROM CODE] | MEDIUM | Master identity; referenced by athlete, org members |
| 17 | **Organization Management** | `OrganizationController /api/organizations` [FROM CODE] | MEDIUM | Club/federation master data; rarely changes |
| 18 | **Member Account Provisioning** | `MemberAccountController` [FROM CODE] | MEDIUM | Create user account; promote to club manager |
| 19 | **Tournament Export / Reporting** | `TournamentExportController /api/exports` [FROM CODE] | LOW | CSV export: entries, schedule, medals |
| 20 | **Scoreboard Views (Display / Judge / Overlay)** | `useScoreboard` hook + `/display`, `/judge`, `/overlay` routes [FROM CODE] | HIGH | Projection screen, kata judge voting, OBS stream overlay |

**Total: 20 features**

---

## SERVICES / MODULES

Architecture is modular-monolith. Grouping unit = **MODULE** (logical bounded context sharing one DB).

| # | Module | Main Package(s) | Est. Entity Count | God-Node? | Outbound Dependencies |
|---|--------|----------------|-------------------|-----------|-----------------------|
| 1 | **Auth & Account** | `*.auth`, `*.security` | 4 (AppUser, Role, UserRoleAssignment, AccountRequest) | **YES — ~30+ edges** (every module calls `CurrentActorProvider`; UserRoleAssignment scopes all permissions) | → Organization (scope_id), Person (identity) |
| 2 | **Organization / Club** | `*.controller` (org), `*.service` (org) | 4 (Organization, OrganizationMember, ClubRoster, Athlete*) | MEDIUM | → Auth (user lookup), Person (identity source) |
| 3 | **Attendance & Fees** | `*.service` (attendance, fee) | 10 (AttendanceSession, AttendanceRecord, AttendanceLeaveRequest, ClubFeeRole, ClubFeeItem, ClubFeeItemRoleAmount, MemberFeeAssignment, OrganizationMemberFeeRole, OrganizationMemberTuitionOverride, ClubFinanceExpense) | MEDIUM — fee rule engine is most complex | → Organization, OrganizationMember, Athlete |
| 4 | **Tournament Operations** | `*.controller` (tournament, category, entry) | 6 (Tournament, TournamentParticipant, Category, Entry, Bracket, CategoryResult) | LOW | → Organization (participant lookup), Athlete (entry eligibility) |
| 5 | **Tatami & Match Scoring** | `*.controller` (match, tatami), `*.realtime` | 7 (Tatami, Match, MatchParticipant, MatchScoreEvent, KumiteMatchState, MatchAuditEvent, KataVote) | MEDIUM — real-time WebSocket publishing | → Tournament (match belongs to tournament), Entry (participants) |
| 6 | **Reporting / Dashboards** | `*.controller` (dashboard, export) | 0 (pure aggregation; no owned tables) | N/A (read-only) | → All modules |
| 7 | **Person (Identity)** | `*.controller` (person), `*.entity` (Person) | 1 (Person) | **YES** (referenced by Athlete, OrganizationMember, AppUser) | — (leaf node, no outbound deps) |

**God nodes confirmed:** `AppUser`/Auth subsystem + `Person` entity. Both are referenced across all modules.
**Highest coupling risk:** Attendance + Fees module (10 entities, complex rule chains, cross-cuts Club + Auth).

---

## DATA MODELS

Architecture = modular-monolith. Grouping unit = **DOMAIN** (feature area). Single DB (`karate_tournament`).

13 Flyway migrations confirm schema history. Entity files exist in `karate-tournament-backend/src/main/java/**/entity/`.

| # | Grouping File (proposed) | Tables Owned | Est. Table Count | Migrations | Has Enums | Cross-domain FKs |
|---|--------------------------|--------------|-----------------|------------|-----------|-----------------|
| 1 | `auth-domain-entities.md` | `users`, `roles`, `user_role_assignments`, `account_requests` | 4 | V1, V3, V11 | YES (SystemRole, AccountRequestStatus) | → `organizations` (scope_id), `persons` (person_id on user) |
| 2 | `person-club-entities.md` | `persons`, `organizations`, `organization_members`, `club_roster`, `athletes` | 5 | V1, V2 | YES (PersonGender, OrganizationType, ClubMemberRole, ClubMemberStatus) | → `users` (user_id on org_member) |
| 3 | `attendance-entities.md` | `attendance_sessions`, `attendance_records`, `attendance_leave_requests`, `club_training_schedules` | 4 | V2, V5, V10 | YES (AttendanceSessionStatus, AttendanceRecordStatus, LeaveRequestStatus, AttendanceSessionType, AttendanceSessionSource) | → `organizations`, `organization_members`, `athletes` |
| 4 | `fee-entities.md` | `club_fee_roles`, `club_fee_items`, `club_fee_item_role_amounts`, `member_fee_assignments`, `organization_member_fee_roles`, `organization_member_tuition_overrides`, `club_finance_expenses` | 7 | V6, V7, V8, V13 | YES (FeeItemType, FeeItemKind, FeeItemStatus, BillingCycle, FeeAssignmentSource, MemberFeeAssignmentStatus) | → `organizations`, `organization_members`, `club_fee_roles` |
| 5 | `tournament-entities.md` | `tournaments`, `tournament_participants`, `categories`, `entries`, `brackets`, `category_results` | 6 | V1, V9, V12 | YES (TournamentStatus, TournamentVisibility, CategoryDiscipline, EntryStatus, BracketType, CompetitionLevel, WeighInStatus, RulesetPreset) | → `organizations` (owner, participant), `athletes` (entry) |
| 6 | `match-entities.md` | `tatamis`, `matches`, `match_participants`, `match_score_events`, `kumite_match_state`, `match_audit_events`, `kata_votes` | 7 | V9 | YES (MatchStatus 9 states, Side, WinType, ScoreEventType, TatamiStatus) | → `tournaments`, `categories`, `entries`, `users` (recordedBy) |

**Total: 34 entities across 6 domain files + 1 overview file**

**Cross-domain FK clusters (coupling risks):**
- `organization_members.user_id` → `users` (Auth → Club)
- `entries.athlete_id` → `athletes` (Tournament → Club)
- `match_score_events.recorded_by_user_id` → `users` (Match → Auth)
- `attendance_leave_requests.decided_by_user_id` → `users` (Attendance → Auth)
- `tournament_participants.organization_id` → `organizations` (Tournament → Club)

---

## CLIENT RULES

Source: CLAUDE.md, code patterns, enum values, controller annotations.

| # | Rule Area | Discovery | Notes |
|---|-----------|-----------|-------|
| 1 | **Org-scoped data isolation** | All org-facing endpoints nest under `/organizations/{id}` [FROM CODE] | CLUB_MANAGER sees only their org; GLOBAL_ADMIN sees all |
| 2 | **Business logic ownership (FE vs BE)** | BillingCycle fix + CLAUDE.md Rule 1 [FROM CODE] | Due dates, billing cycles, fee kind inference → BE always |
| 3 | **Flyway migration safety** | CLAUDE.md Rule 2 [FROM CODE] | Never edit applied migrations; add new version only |
| 4 | **Never expose production credentials / PII** | Inferred from standard practice [ASSUMPTION] | Seed data in V4 (dev-only); no PII in logs |
| 5 | **Vietnamese market specifics** | `CompetitionLevel.PHONG_TRAO / NANG_CAO`, V8 (Vietnamese role labels) [FROM CODE] | Labels and business tier names are in Vietnamese |
| 6 | **Realtime invariant** | After any match mutation → `RealtimePublisher.publishMatch()` [FROM CODE] | Skipping this breaks all connected display/judge screens |
| 7 | **Domain invariant chain** | CLAUDE.md [FROM CODE] | `Person → OrganizationMember → Athlete/ClubRoster → Entry` — cannot skip steps |
| 8 | **Auth propagation** | `CurrentActorProvider` injected into services [FROM CODE] | Do not use `SecurityContextHolder` directly in service layer |

---

## KNOWN ISSUES

Source: fixed bugs, complex code areas, state machine depth, job scheduling risks.

| # | Area | Issue | Severity | Source |
|---|------|-------|----------|--------|
| 1 | **BillingCycle logic leak** | FE was computing due dates from client clock + hardcoding billingCycle defaults — fixed 2026-06-11; pattern may recur if devs add FE-side "convenience" logic | MEDIUM | [FROM CODE] — see `context/known-issues/billing-cycle-leak.md` |
| 2 | **MatchStatus state machine (9 states)** | `SCHEDULED → READY → RUNNING → PAUSED → REVIEW → HANTEI → VOTING → COMPLETED → LOCKED` — transitions not all documented; invalid transitions may silently succeed or throw unhandled | HIGH | [FROM CODE] — `MatchStatus` enum + `MatchController` |
| 3 | **ClubTrainingScheduleJob race condition** | Auto-generates attendance sessions on scheduled days; if job runs twice (restart, duplicate trigger) it may create duplicate sessions. No idempotency guard visible | MEDIUM | [FROM CODE] — `ClubTrainingScheduleJob` + `/ensure-today` endpoint |
| 4 | **Person / AppUser identity duality** | Two separate identity entities: `Person` (contact info) and `AppUser` (system account). Not all `Person` records have a corresponding `AppUser`. Code that assumes `personId == userId` will be wrong | HIGH | [FROM CODE] — `Athlete` links to `Person`; `OrganizationMember` has both `person_id` and `user_id` (nullable) |
| 5 | **Fee assignment rule engine** | `fee-items/{id}/apply` applies fees to all matching role members in batch; if partial failure occurs mid-batch, rollback behavior unclear. No explicit retry or idempotency key | HIGH | [FROM CODE] — `ClubFeeServiceImpl`, `MemberFeeAssignment` |
| 6 | **`V4__mock_club_data.sql` in production migration chain** | Seed/dev data migration is in the same Flyway sequence as schema migrations. If applied in a non-dev environment it pollutes production data | MEDIUM | [FROM CODE] — V4 filename suggests dev-only data |
| 7 | **Entry weight validation gap** | `Entry` has `registrationWeightKg` and `weighInStatus` but no visible constraint enforcing that `registrationWeightKg` is within `Category.weightMinKg–weightMaxKg` at the DB or service layer | MEDIUM | [ASSUMPTION] — inferred from field co-location; needs verification |

---

## FILES TO GENERATE (Phase 2 plan)

| File | Type | Status |
|------|------|--------|
| `context/architecture/overview.md` | architecture | TODO |
| `context/features/auth-account.md` | feature | TODO |
| `context/features/club-roster-membership.md` | feature | TODO |
| `context/features/training-attendance.md` | feature | TODO |
| `context/features/club-finance-fees.md` | feature | TODO |
| `context/features/tournament-operations.md` | feature | TODO |
| `context/features/tatami-match-scoring.md` | feature | TODO |
| `context/features/member-self-service.md` | feature | TODO |
| `context/features/draw-brackets.md` | feature | TODO |
| `context/api/club-api.md` | api | TODO |
| `context/api/tournament-api.md` | api | TODO |
| `context/api/match-api.md` | api | TODO |
| `context/services/auth-module.md` | service | TODO |
| `context/services/club-module.md` | service | TODO |
| `context/services/attendance-fees-module.md` | service | TODO |
| `context/services/tournament-module.md` | service | TODO |
| `context/services/match-tatami-module.md` | service | TODO |
| `context/services/business-fee-service.md` | service (biz) | TODO |
| `context/services/business-match-service.md` | service (biz) | TODO |
| `context/governance/coding-standards.md` | governance | EXISTS (stub) |
| `context/governance/api-standards.md` | governance | TODO |
| `context/client-rules/client-specific-rules.md` | client-rules | TODO |
| `context/known-issues/patterns.md` | known-issue | TODO |
| `context/data-models/data-model-overview.md` | data-model | TODO |
| `context/data-models/auth-domain-entities.md` | data-model | TODO |
| `context/data-models/person-club-entities.md` | data-model | TODO |
| `context/data-models/attendance-entities.md` | data-model | TODO |
| `context/data-models/fee-entities.md` | data-model | TODO |
| `context/data-models/tournament-entities.md` | data-model | TODO |
| `context/data-models/match-entities.md` | data-model | TODO |
| `context/indexes/context-index.md` | index | EXISTS (stub) |

**Total files to generate: ~30**

---

## QUESTIONS NEEDING HUMAN INPUT (Phase 1 blockers)

| Priority | Question | Who can answer |
|----------|----------|----------------|
| P1 | Is `V4__mock_club_data.sql` intentionally in the production migration chain, or should it be excluded for non-dev environments? | Repo owner |
| P1 | What are the valid `MatchStatus` state transitions? (e.g. can RUNNING go directly to COMPLETED, or must pass through REVIEW?) | Product/tech lead |
| P2 | Is there a refresh token mechanism, or are JWTs long-lived? What is the configured expiry? | Backend dev |
| P2 | Is `Person` always required before `AppUser`, or can a user exist without a Person record? | Backend dev |
| P2 | `CompetitionLevel.PHONG_TRAO / NANG_CAO` — are there other region/market-specific rules branching on this? | Product owner |
| P3 | Are there any client-specific PII handling rules (data retention, export restrictions)? | Project/compliance owner |
| P3 | Is `ClubTrainingScheduleJob` idempotent? What guard prevents duplicate session creation? | Backend dev |
