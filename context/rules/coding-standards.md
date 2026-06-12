---
name: coding-standards
description: Project coding rules — BE/FE boundary, migration safety, reference conventions, review gate, lane separation
type: rules
version: "1.0"
last_updated: "2026-06-11"
criticality: CRITICAL
metadata:
  owner: tech-lead
  affected_services: [karate-ops-fe, karate-tournament-backend]
  affected_domains: [all]
source_references:
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/service/impl/ClubFeeServiceImpl.java, note: "defaultDueDate(), inferFeeKind()" }
  - { file_path: karate-ops-fe/src/features/clubs/clubApi.ts, note: "BillingCycle boundary — FE sends null" }
---

# Coding Standards

## Rule 1 — Business logic belongs in BE, never FE

**Scope**: due date computation, billing cycle defaulting, fee kind inference, any domain rule that determines "what the correct value should be."

**Why**: FE was computing due dates from the client clock and hardcoding `BillingCycle` defaults — duplicating `ClubFeeServiceImpl.inferFeeKind()`. A BE rule change would leave FE silently wrong. Discovered via BillingCycle bridge-node trace in the knowledge graph. See [billing-cycle-leak.md](../known-issues/billing-cycle-leak.md).

**Pattern**:
- BE `defaultDueDate()` uses `LocalDate.now()` server-side; clamps dueDay to [1, 28]; defaults to 10
- BE infers `billingCycle` when FE sends null: `ONE_TIME_INCOME → ONE_TIME`, else `MONTHLY`
- FE sends `null` for fields it doesn't own; BE fills defaults

**Red flag**: Any FE code that reads `feeKind` or `feeType` to decide what value to send upstream.

---

## Rule 2 — Never edit applied Flyway migrations

Add a new `V{n}__{description}.sql`. Editing history corrupts every environment that already applied it.

**Current risk**: V4 contains dev seed data in the production migration chain. Do not fix by editing V4 — add a V14+ rollback migration instead. See [known-issues/patterns.md Pattern 6].

---

## Rule 3 — Symbol references in context files, not line numbers

`source_references` use `file_path` + optional `note` describing the symbol.  
Line numbers rot after the first refactor. Use symbol names (class, method) that survive renaming only if the symbol is renamed too.

---

## Rule 4 — Context updates require a human review gate

AI-generated context files require human review before merge.  
Unreviewed context compounds into trusted-but-wrong state — worse than no context.

**Process**: Context changes go in a separate PR. Reviewer checks `[LOW CONFIDENCE]` markers and `[NEEDS HUMAN INPUT]` items.

---

## Rule 5 — Lane separation: graph vs context/

| Fact type | Source |
|-----------|--------|
| What imports what, call graph, where X is defined | `graphify` (`/graphify query "..."`) |
| Business rule, convention, client requirement | `context/rules/` |
| Architecture decision | `context/architecture/` |
| Feature flow, domain logic | `context/features/` |
| Bug pattern, fragile area | `context/known-issues/` |

Never maintain relationship metadata by hand — graphify re-derives from AST on every `--update`.

---

## Rule 6 — Soft delete only

Never issue physical `DELETE` against any entity table. Call `entity.softDelete()` which sets `deletedAt = Instant.now()`. All repositories must filter `WHERE deleted_at IS NULL` (or use JPA `@Where` annotation).

---

## Rule 7 — Domain invariant chain must be respected

Cannot skip steps in:
```
Person → OrganizationMember → Athlete → ClubRoster → TournamentParticipant → Entry
```
Creating an `Athlete` without a `Person`, or adding an `Entry` without a `ClubRoster`, throws `BusinessConflictException`. This is enforced in service layer (not DB constraints alone).

---

## Rule 8 — Realtime publish after every match mutation

After any match state change (score, penalty, timer, result):
```java
realtimePublisher.publishMatch(matchResponse);
```
Forgetting this leaves all tatami views stale. There is no retry — if publish is skipped, clients stay frozen until next event.

---

## Rule 9 — Exception hierarchy (never throw RuntimeException raw)

| Situation | Throw |
|-----------|-------|
| Resource not found | `ResourceNotFoundException` |
| Invalid input / validation failure | `BadRequestException` |
| Domain rule violation | `BusinessConflictException` |
| Insufficient permission | `ForbiddenException` |
| Authentication failure | `UnauthorizedException` |

All are subclasses of `ApiException` and are handled by `ApiExceptionHandler` → proper `ApiResponse<T>` error shape.

---

## Rule 10 — Graph re-sync after source changes

After adding, removing, or significantly modifying source files:
```
/graphify . --update
```
Code-only changes use free AST extraction. Doc/config changes trigger semantic re-extraction. Do not edit `graphify-out/` by hand.
