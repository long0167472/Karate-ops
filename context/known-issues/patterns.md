---
name: known-issue-patterns
description: Confirmed bug patterns, fragile areas, and workarounds across the codebase
type: known-issue
version: "1.0"
last_updated: "2026-06-11"
criticality: HIGH
metadata:
  owner: tech-lead
  affected_services: [karate-ops-fe, karate-tournament-backend]
  affected_domains: [fees, match, auth, attendance, tournament]
source_references:
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/service/impl/ClubFeeServiceImpl.java, line_range: "1-end", note: "Fee assignment batch, amount resolution" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/service/impl/MatchServiceImpl.java, line_range: "1-end", note: "Match state machine, score events" }
  - { file_path: karate-tournament-backend/src/main/resources/db/migration/V4__mock_club_data.sql, line_range: "1-end", note: "Dev seed in prod migration chain" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/job/ClubTrainingScheduleJob.java, line_range: "1-end", note: "Scheduled session generator" }
knowledge_graph_refs:
  - { community: "fees", hub_node: "MemberFeeAssignment" }
  - { community: "match-execution", hub_node: "Match" }
related_context_files:
  - context/known-issues/billing-cycle-leak.md
  - context/features/club-finance-fees.md
  - context/features/tatami-match-scoring.md
---

# Known Issue Patterns

## Pattern 1 — BillingCycle / Business Logic Leak (RESOLVED)

**Area**: Fee system — `FeesTab.tsx` + `ClubFeeServiceImpl`
**Status**: Fixed 2026-06-11
**Root cause**: FE was computing due dates from client clock and hardcoding `billingCycle` defaults, duplicating BE's `inferFeeKind()`.
**Current state**: BE owns all defaults. FE sends null; BE fills via `defaultDueDate()` and `inferFeeKind()`.
**Recurrence risk**: HIGH — any developer adding "convenience" FE logic that touches fee amounts, dates, or cycles will reintroduce this.
**Detection**: Run graphify; if `BillingCycle` appears as a bridge node again between FE and BE communities → leak has recurred.
**See**: [billing-cycle-leak.md](./billing-cycle-leak.md)

---

## Pattern 2 — MatchStatus State Machine Has No Transition Guard

**Area**: Match scoring — `MatchServiceImpl.recordEvent()`
**Severity**: HIGH
**What happens**: `MatchStatus` has 10 states: `SCHEDULED, READY, RUNNING, PAUSED, REVIEW, HANTEI, VOTING, LOCKED, ABANDONED, CANCELLED`. The `recordEvent()` method transitions status on certain event types (TIMER_START → RUNNING, TIMER_STOP → PAUSED, STATUS_CHANGE → target status) but does not validate that the current status is a valid predecessor.

**Risk**: A tatami operator can send `TIMER_START` on a `LOCKED` or `COMPLETED` match, potentially re-opening a decided match.

**Workaround**: Frontend control desk UI prevents this via button state. But direct API calls bypass this guard.

**Fix needed**: Add explicit state transition validation in `MatchServiceImpl.recordEvent()`:
```java
if (match.status == MatchStatus.LOCKED || match.status == MatchStatus.CANCELLED) {
    throw new BusinessConflictException("Cannot record events on a " + match.status + " match");
}
```
**[NEEDS HUMAN INPUT]**: What are the valid state transitions for each status? Is PAUSED → HANTEI valid? Can REVIEW go back to RUNNING?

---

## Pattern 3 — Fee Assignment Batch Has No Idempotency Key

**Area**: `ClubFeeServiceImpl.applyFeeItem()`
**Severity**: HIGH
**What happens**: `POST /fee-items/{id}/apply` creates `MemberFeeAssignment` records for all target members in a single `@Transactional` method. The only duplicate guard is a check for `(member, feeItem)` pair existence:
```java
// Skip if assignment already exists
if (existingByMemberAndItem.contains(member.id + ":" + item.id)) continue;
```
**Risk 1 (partial failure)**: If the transaction fails mid-batch (e.g., DB connection drop on member #50 of 200), partial assignments are rolled back but the caller sees a 500. They may retry, creating a logic race.

**Risk 2 (concurrent calls)**: Two simultaneous calls to `/apply` for the same item before either has committed can both pass the existence check, creating duplicate assignments.

**Workaround**: Currently relies on unique DB constraint. If duplicates appear, manually delete duplicate `MemberFeeAssignment` records.

**Fix needed**: Add DB unique constraint on `(organization_member_id, fee_item_id)` WHERE `deleted_at IS NULL`.

---

## Pattern 4 — Person / AppUser Identity Duality

**Area**: Person management, member provisioning
**Severity**: HIGH
**What happens**: Two separate identity entities exist:
- `Person` — contact information (displayName, birthDate, phone, address, emergency contact)
- `AppUser` — system account (email, username, passwordHash, status)

`OrganizationMember` has both `person_id` (nullable) and `user_id` (nullable). It is valid to have:
- A member with Person but no AppUser (no system login)
- An AppUser with no Person record (admin-provisioned accounts)

**Risk**: Any code that assumes `person.id == user.id` or `personId → userId` will be wrong. The `Athlete` entity links to `Person` (1:1), not `AppUser`. Tournament entries link to `Athlete → Person`, not to any user account.

**Workaround**: Always trace through the chain: `OrganizationMember.person` and `OrganizationMember.user` separately. Never substitute one for the other.

**Detection**: Search for `person.id` being compared to `user.id` or used interchangeably.

---

## Pattern 5 — ClubTrainingScheduleJob Idempotency (CONFIRMED SAFE)

**Area**: `ClubTrainingScheduleJob` + `AttendanceSession` creation
**Severity**: RESOLVED (idempotency confirmed in code)
**What happens**: The job runs at `00:05 Asia/Ho_Chi_Minh` daily and also on `ApplicationReadyEvent` (startup).

**Idempotency is present**: `ensureSessionForDate()` queries for an existing session with `(trainingSchedule, source=SCHEDULED, scheduledDate=today)` before creating. If one is found, it returns 0 — no duplicate is created.

The endpoint `POST /training-schedule/ensure-today` is safe to call multiple times per day.

---

## Pattern 6 — V4 Dev Seed Data in Production Migration Chain

**Area**: Flyway migrations — `V4__mock_club_data.sql`
**Severity**: MEDIUM (environment-dependent)
**What happens**: V4 is positioned between V3 (auth credentials) and V5 (training schedule) in the Flyway sequence. It inserts demo clubs, members, and athletes. Any environment that runs Flyway from scratch (including production) will execute V4.

**Risk**: Production database will contain demo records named "Câu lạc bộ Karate Demo" or similar. These cannot be rolled back without a new migration.

**[NEEDS HUMAN INPUT]**: Was V4 intentionally included for all environments? If not, it must be wrapped in a conditional (`IF current_setting('app.env', true) = 'dev'`) or moved to a separate seed script.

**Workaround**: Run `SELECT COUNT(*) FROM organizations WHERE name LIKE '%Demo%'` on prod after deployment to check for contamination.

---

## Pattern 7 — Temporary Password Transmitted in Plaintext

**Area**: `AccountProvisioningServiceImpl`, `AccountRequestServiceImpl`, `MemberAccountController`
**Severity**: MEDIUM (requires HTTPS)
**What happens**: `AccountProvisioningService.DEFAULT_TEMPORARY_PASSWORD = "123456"` is a hardcoded constant. It is returned in `MemberAccountCreateResponse.temporaryPassword` and sent via `AccountNotificationService`.

**Risk 1**: If the API is accessed over HTTP (not HTTPS), the password is exposed in transit.
**Risk 2**: "123456" is trivially guessable if the user never changes it.

**Workaround**: Ensure HTTPS in production. Notify users immediately after provisioning. Consider adding a `passwordChangeRequired` flag to `AppUser` that forces password change on first login.

---

## Pattern 8 — Entry Weight Validation Does Not Block Registration

**Area**: `CategoryServiceImpl.addEntry()`
**Severity**: MEDIUM
**What happens**: When registering an entry, the code calculates `WeighInStatus` based on weight:
- `VALID` if weight within category bounds
- `OUT_OF_CLASS` if outside bounds
- `MISSING_WEIGHT` if no weight provided
- `NEEDS_ORGANIZER_REVIEW` (default)

However, the code **does not throw an exception** when `weighInStatus = OUT_OF_CLASS`. The entry is created with `OUT_OF_CLASS` status and a validation note. The organizer must manually review.

**Risk**: An athlete outside the weight class can be entered and will appear in the draw unless manually removed.

**Workaround**: Filter entries in the draw step; entries with `weighInStatus = OUT_OF_CLASS` or `status = WITHDRAWN` are excluded from `DrawServiceImpl.draw()`.

---

## Cross-Cutting Pattern: Unbounded List Queries

**Area**: All `list()` endpoints
**Severity**: LOW (currently), will become HIGH as data grows
**What happens**: Every list endpoint returns all records:
```java
tournaments.findByDeletedAtIsNullOrderByStartsOnDescCreatedAtDesc()  // all tournaments
members.findByOrganization_IdAndDeletedAtIsNull(orgId)                // all members
attendanceRecords.findBySession_IdAndDeletedAtIsNull(sessionId)       // all records per session
```

No pagination exists anywhere in the codebase.

**Risk**: `MatchScoreEvent` (many events per match), `AttendanceRecord` (many records per session), and `MemberFeeAssignment` tables will cause slow responses at scale.

**Workaround**: Monitor response times. When a list endpoint exceeds 500ms, prioritize adding `Page<T>` support to that endpoint. Start with `MatchScoreEvent` and `AttendanceRecord`.
