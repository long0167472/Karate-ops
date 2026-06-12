---
name: coverage-audit
description: Phase 4 coverage audit — planned vs generated files, thin coverage, open questions, and NEEDS HUMAN INPUT items
type: index
last_updated: "2026-06-11"
---

# Phase 4 — Coverage Audit

Generated: 2026-06-11. Compares `_expected-inventory.md` (Phase 1) vs actual generated files.

---

## File Coverage: Planned vs Generated

### ✅ COMPLETE (26/31 planned files)

| Planned | Actual File | Notes |
|---------|-------------|-------|
| `context/architecture/overview.md` | `architecture/overview.md` | |
| `context/features/auth-account.md` | `features/auth-account.md` | |
| `context/features/club-roster-membership.md` | `features/club-roster-membership.md` | |
| `context/features/training-attendance.md` | `features/training-attendance.md` | |
| `context/features/club-finance-fees.md` | `features/fee-system.md` | Name differs — same content |
| `context/features/tournament-operations.md` | `features/tournament-operations.md` | |
| `context/features/tatami-match-scoring.md` | `features/tatami-match-scoring.md` | |
| `context/features/member-self-service.md` | `features/member-self-service.md` | |
| `context/features/draw-brackets.md` | `features/draw-brackets.md` | |
| `context/api/club-api.md` | `api/club-api.md` | |
| `context/api/tournament-api.md` | `api/tournament-api.md` | |
| `context/api/match-api.md` | `api/match-api.md` | |
| `context/services/auth-module.md` | `services/auth-module.md` | |
| `context/services/business-fee-service.md` | `services/business-fee-service.md` | |
| `context/governance/api-standards.md` | `governance/api-standards.md` | |
| `context/governance/coding-standards.md` | `rules/coding-standards.md` | Location differs — content present |
| `context/client-rules/client-specific-rules.md` | `client-rules/client-specific-rules.md` | |
| `context/known-issues/patterns.md` | `known-issues/patterns.md` | |
| `context/data-models/data-model-overview.md` | `data-models/data-model-overview.md` | |
| `context/data-models/auth-domain-entities.md` | `data-models/auth-domain-entities.md` | |
| `context/data-models/person-club-entities.md` | `data-models/person-club-entities.md` | |
| `context/data-models/attendance-entities.md` | `data-models/attendance-entities.md` | |
| `context/data-models/fee-entities.md` | `data-models/fee-entities.md` | |
| `context/data-models/tournament-entities.md` | `data-models/tournament-entities.md` | |
| `context/data-models/match-entities.md` | `data-models/match-entities.md` | |
| `context/indexes/context-index.md` | `indexes/context-index.md` | |

### ❌ MISSING (5 planned files not generated)

| Planned File | Priority | Why missing | Recommended action |
|-------------|----------|-------------|-------------------|
| `context/services/club-module.md` | LOW | Data already covered in `club-api.md` and `person-club-entities.md` | Create thin stub pointing to those files |
| `context/services/attendance-fees-module.md` | LOW | Content spread across `training-attendance.md` and `business-fee-service.md` | Create thin stub pointing to those files |
| `context/services/tournament-module.md` | LOW | Covered by `tournament-operations.md` | Create thin stub |
| `context/services/match-tatami-module.md` | LOW | Covered by `tatami-match-scoring.md` and `match-api.md` | Create thin stub |
| `context/services/business-match-service.md` | MEDIUM | Match service logic covered in `tatami-match-scoring.md` but no dedicated service file | Create from `MatchServiceImpl` if deeper detail needed |

### ➕ EXTRA (not planned, but generated)

| Extra File | Value |
|-----------|-------|
| `architecture/frontend.md` | HIGH — covers React routing, apiClient, tatami views |
| `architecture/backend.md` | HIGH — covers Spring Boot structure, auth flow |
| `known-issues/billing-cycle-leak.md` | MEDIUM — specific issue with resolution |
| `indexes/_expected-inventory.md` | HIGH — Phase 1 inventory baseline |

---

## Feature Coverage vs Inventory (20 features)

| # | Feature | Coverage | Status |
|---|---------|----------|--------|
| 1 | Auth — Login & JWT | `features/auth-account.md` + `services/auth-module.md` | ✅ FULL |
| 2 | Club Member Onboarding | `features/auth-account.md` | ✅ FULL |
| 3 | Club Roster & Membership | `features/club-roster-membership.md` | ✅ FULL |
| 4 | Training Attendance | `features/training-attendance.md` | ✅ FULL |
| 5 | Club Finance — Fees | `features/fee-system.md` + `services/business-fee-service.md` | ✅ FULL |
| 6 | Tournament Admin | `features/tournament-operations.md` + `api/tournament-api.md` | ✅ FULL |
| 7 | Categories & Entries | `features/tournament-operations.md` | ✅ FULL |
| 8 | Live Match Control | `features/tatami-match-scoring.md` + `api/match-api.md` | ✅ FULL |
| 9 | Tatami Setup & Assignment | `api/tournament-api.md` (Tatami section) | ⚠️ THIN — no dedicated feature file |
| 10 | Member Self-Service Portal | `features/member-self-service.md` | ✅ FULL |
| 11 | Club Training Schedule | `features/training-attendance.md` | ✅ FULL |
| 12 | Club Dashboard & Analytics | — | ❌ MISSING |
| 13 | Tournament Dashboard | — | ❌ MISSING |
| 14 | Draw & Brackets | `features/draw-brackets.md` | ✅ FULL |
| 15 | Athlete Management | `features/club-roster-membership.md` (partial) | ⚠️ THIN |
| 16 | Person (Identity) Management | `data-models/person-club-entities.md` | ⚠️ THIN |
| 17 | Organization Management | `api/club-api.md` (Organizations section) | ⚠️ THIN |
| 18 | Member Account Provisioning | `features/auth-account.md` | ✅ FULL |
| 19 | Tournament Export / Reporting | — | ❌ MISSING — LOW priority |
| 20 | Scoreboard Views (Display/Judge/Overlay) | `features/tatami-match-scoring.md` (Tatami Views section) | ⚠️ THIN |

**Coverage summary**: 11 FULL, 5 THIN, 3 MISSING

---

## Known Issues Coverage

| # | Issue | Status |
|---|-------|--------|
| 1 | BillingCycle logic leak | ✅ Documented + RESOLVED |
| 2 | MatchStatus state machine | ✅ Documented in `patterns.md` Pattern 2 |
| 3 | ClubTrainingScheduleJob race | ✅ Pattern 5 — CORRECTED: idempotency IS present |
| 4 | Person/AppUser identity duality | ✅ Documented in `patterns.md` Pattern 4 |
| 5 | Fee assignment batch idempotency | ✅ Documented in `patterns.md` Pattern 3 |
| 6 | V4 seed in prod migration chain | ✅ Documented in `patterns.md` Pattern 6 |
| 7 | Entry weight validation gap | ✅ Documented in `patterns.md` Pattern 8 |

---

## Open Questions (NEEDS HUMAN INPUT)

Prioritized list for the next human review session:

| Priority | Question | Context File | Notes |
|----------|----------|-------------|-------|
| **P1** | Is `V4__mock_club_data.sql` intentionally in production migration chain? If yes, has it been applied to prod already? | `known-issues/patterns.md` Pattern 6 | Risk: pollutes prod DB with dev org/user data |
| **P1** | What are the full valid MatchStatus transitions? Can RUNNING go directly to LOCKED, or must pass through COMPLETED? | `data-models/match-entities.md`, `features/tatami-match-scoring.md` | No transition guard confirmed — any state change is possible |
| **P2** | Are `ROUND_ROBIN` and `POOL` bracket types on the roadmap? Currently not implemented in `DrawServiceImpl` | `features/draw-brackets.md` | Enum values exist; no draw logic |
| **P2** | What is the `ClubFeeItemRoleAmount.amount` format — absolute value in VND, or percentage? Default role amounts after V7/V8 migration — are they 100/80/60/40 or something else? | `data-models/fee-entities.md`, `services/business-fee-service.md` | Affects fee role interpretation |
| **P2** | Is there a refresh token mechanism, or are JWT sessions single-token? Default TTL is 480 minutes (8 hours) — is this intentional for the target use case? | `services/auth-module.md` | No refresh logic found in code |
| **P2** | `CompetitionLevel.PHONG_TRAO / NANG_CAO` — are there product rules that branch differently between these levels beyond label display? | `features/tournament-operations.md` | Used as CSV on Tournament; may affect scoring rules or bracket defaults |
| **P3** | Are there PII handling rules for data export or retention? `Person.nationalId` is stored in plaintext | `data-models/person-club-entities.md` | Vietnamese PDPD compliance risk |
| **P3** | `TournamentDashboardController` / `OrganizationDashboardController` — what data do these return? No context file exists for dashboards | — | Features 12 & 13 in inventory are undocumented |
| **P3** | `TournamentExportController` — does it exist? What formats does it export? | — | Feature 19 in inventory; LOW priority |
| **P3** | `Team` entity — `Entry.teamId` is a UUID but no `Team` entity file was found. Is team management fully implemented? | `data-models/tournament-entities.md` | `MatchParticipant.team` field also flagged |

---

## Self-Critique Corrections Applied (Phase 3)

The following errors were found and corrected during Phase 3:

| Error | Correction | Files Fixed |
|-------|-----------|-------------|
| `MatchStatus` had `ABANDONED` | Correct value is `COMPLETED` | `data-models/data-model-overview.md`, `data-models/match-entities.md`, `features/tatami-match-scoring.md` |
| `TatamiStatus` was `[NEEDS HUMAN INPUT]` | Actual values: `ACTIVE, INACTIVE` | `data-models/match-entities.md` |
| `ExpenseDisbursementStatus` was `[NEEDS HUMAN INPUT]` | Actual values: `PENDING_DISBURSEMENT, DISBURSED` | `data-models/fee-entities.md` |
| `MemberFeeAssignmentStatus` had `CANCELLED` | The enum is `PaymentStatus` with no `CANCELLED` value | `data-models/fee-entities.md`, `services/business-fee-service.md` |
| `ClubTrainingScheduleJob` idempotency: "no guard" | Idempotency IS present — `ensureSessionForDate()` queries before creating | `features/training-attendance.md`, `known-issues/patterns.md` Pattern 5 |
| `ROUND_ROBIN`/`POOL` brackets marked LOW CONFIDENCE | Confirmed NOT implemented in `DrawServiceImpl` | `features/draw-brackets.md` |

---

## Recommended Next Steps

1. **Human review gate**: Review all files with `[LOW CONFIDENCE]` markers before use in production tasks
2. **Answer P1 questions**: V4 migration risk and MatchStatus transitions need repo owner input
3. **Create thin stubs** for 5 missing service module files (low priority)
4. **Add dashboard coverage**: Create `features/tournament-dashboard.md` and `features/club-dashboard.md`
5. **Run `/graphify . --update`** to sync graph after all context files added
6. **Commit context/ folder** to git for version control
