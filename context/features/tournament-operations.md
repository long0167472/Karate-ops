---
name: tournament-operations
description: Tournament lifecycle — creation, delegation management, category setup, entry registration, and bracket draw
type: feature
version: "1.0"
last_updated: "2026-06-11"
criticality: HIGH
metadata:
  owner: backend
  affected_services: [karate-tournament-backend, karate-ops-fe]
  affected_domains: [tournaments, clubs]
source_references:
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/web/TournamentController.java, line_range: "1-end", note: "Tournament CRUD + participant management" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/web/CategoryController.java, line_range: "1-end", note: "Category CRUD, entry management, draw" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/service/impl/TournamentServiceImpl.java, line_range: "1-end", note: "Tournament business logic" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/service/impl/CategoryServiceImpl.java, line_range: "1-end", note: "Entry validation, category rules" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/service/impl/DrawServiceImpl.java, line_range: "1-end", note: "Bracket generation algorithm" }
knowledge_graph_refs:
  - { community: "tournament-operations", hub_node: "Tournament" }
  - { community: "club-operations", hub_node: "Organization" }
related_context_files:
  - context/features/tatami-match-scoring.md
  - context/features/draw-brackets.md
  - context/data-models/tournament-entities.md
---

# Feature: Tournament Operations

## Actors
- **Club Manager / Tournament Owner** — creates and manages tournaments for their organization
- **Global Admin** — can manage all tournaments
- **Delegation Manager** — approves/manages their organization's participation

## Flow 1: Tournament Creation

```
POST /api/tournaments
{
  name (required), code, description, location,
  startsOn, endsOn, ownerOrganizationId,
  visibility, tatamiCount, competitionLevels,
  rulesetVersion, rulesetPreset
}
```

**Permission**: `requireTournamentCreate(ownerOrganizationId)` — CLUB_MANAGER must own the org.

**Defaults applied by service**:
- `ownerOrganizationId` → `actor.primaryOrganizationId()` if null
- `visibility` → `PRIVATE`
- `status` → `DRAFT`
- `rulesetVersion` → `WKF_2026`
- `tatamiCount` → `1` (must be ≥ 1)
- `competitionLevels` → `["PHONG_TRAO", "NANG_CAO"]` (normalized to CSV in DB)
- `rulesetPreset` → `WKF`
- `ruleSnapshotJson` → auto-generated from preset (JSON rules snapshot)
- `createdByUser` → current actor

**TournamentStatus lifecycle**: `DRAFT → REGISTRATION_OPEN → REGISTRATION_CLOSED → DRAWING → RUNNING → COMPLETED → ARCHIVED`

**TournamentVisibility**: `PUBLIC, PRIVATE, INVITE_ONLY`

## Flow 2: Delegation Management

Organizations participating as delegations must be added to the tournament:

**Add delegation**:
```
POST /api/tournaments/{id}/participants
{ organizationId (required), displayName?, status? }
```
- Default `status = REQUESTED`
- Default `displayName = organization.name`
- Unique constraint on `(tournament_id, organization_id)` — duplicate → `BusinessConflictException`
- `approvedAt` set automatically if `status = APPROVED`

**Update delegation status**:
```
PATCH /api/tournaments/{id}/participants/{pId}/status
{ status (required) }
```
`ParticipantStatus` values: `INVITED, REQUESTED, APPROVED, REJECTED, WITHDRAWN`

**Business rule**: Entries can only be added for `APPROVED` delegations. `CategoryServiceImpl.addEntry()` checks `participant.status == APPROVED`.

## Flow 3: Category Setup

A tournament has multiple categories (competition events):

```
POST /api/tournaments/{id}/categories
{
  name (required), discipline (required),
  gender, ageMin, ageMax, weightMinKg, weightMaxKg,
  competitionLevel, openWeight, entryType,
  repechageEnabled, matchDurationSeconds,
  kataJudgeCount, kataRepeatAllowed
}
```

**CategoryDiscipline**: `KUMITE, KATA, PARA_KATA, TEAM_KUMITE, TEAM_KATA`

**Rules applied by `applyCategoryRules()`**:
- `gender` defaults to `OPEN`
- `entryType` defaults to `INDIVIDUAL`
- `openWeight = true` if kumite and no weight bounds set
- `weightLabel` auto-generated: e.g., `"-60kg"`, `"+84kg"`, `"Vô địch tuyệt đối"` for open weight
- `repechageEnabled` defaults to `true`
- `matchDurationSeconds` defaults to `180` (minimum 30)
- `kataJudgeCount` must be exactly `5` or `7`
- `competitionLevel` defaults to `OPEN`

**Category status**: String field, starts as `"DRAFT"`, set to `"DRAWN"` after bracket generation.

## Flow 4: Entry Registration

```
POST /api/categories/{id}/entries
{
  tournamentParticipantId (required),
  athleteId (for INDIVIDUAL), teamId/teamName (for TEAM),
  seedNo, registrationWeightKg
}
```

**Validation chain in `CategoryServiceImpl.addEntry()`**:
1. `participant.status == APPROVED` (throws `BusinessConflictException` if not)
2. `participant.tournament.id == category.tournament.id`
3. Athlete must be on `ClubRoster` of participant's organization (via `ClubRosterService`)
4. Athlete not already in this category
5. Athlete gender matches category gender (if category is not OPEN)
6. Athlete age within `ageMin`/`ageMax` bounds
7. Athlete weight within `weightMinKg`/`weightMaxKg` — does NOT reject, sets `weighInStatus = OUT_OF_CLASS`
8. Entry limit per org enforced if `entryLimitPerOrganization` set

**Entry defaults**:
- `registrationWeightKg` → `athlete.weightKg` if null
- `weighInStatus` → computed from weight validation
- `status` → `REGISTERED`

**EntryStatus**: `REGISTERED, CHECKED_IN, WITHDRAWN, DISQUALIFIED`
**WeighInStatus**: `VALID, MISSING_WEIGHT, OUT_OF_CLASS, NEEDS_ORGANIZER_REVIEW`

## Flow 5: Draw Generation

**Trigger**: `POST /api/categories/{id}/draw { shuffle?, enableRepechage? }`

**Pre-conditions**: Category must not already have matches (throws `BusinessConflictException`).

**See**: [draw-brackets.md](./draw-brackets.md) for full algorithm.

**Post-state**: `category.status = "DRAWN"`, matches created, bracket linked.

## Business Rules

1. **Category cannot be modified after draw** — `ensureCategoryOpenForEntries()` throws `BusinessConflictException` if `status == "DRAWN"` or matches exist.
2. **Entry deletion blocked after draw** — same guard applies to `DELETE /categories/{id}/entries/{entryId}`.
3. **Delegation must be APPROVED before entry** — `REQUESTED` or `REJECTED` delegations cannot add entries.
4. **Weight OUT_OF_CLASS does not block** — entry is created with a warning; organizer must manually review and remove if needed.

## Edge Cases

| Scenario | Behavior |
|----------|----------|
| Duplicate delegation (org already in tournament) | `BusinessConflictException` |
| Entry for non-rostered athlete | `BusinessConflictException` from ClubRosterService |
| Draw with < 2 entries | `BusinessConflictException("Not enough entries to generate a bracket")` |
| Draw with exactly 1 entry (BYE) | BYE entry auto-advances; final match has 1 participant |
| Delete entry after draw | `BusinessConflictException` |

## Regional Variants
- `CompetitionLevel.PHONG_TRAO` / `NANG_CAO` — Vietnamese-specific tiers; `OPEN` also available
- Default `competitionLevels` CSV for new tournaments: `"PHONG_TRAO,NANG_CAO"`
- `weightLabel` uses Vietnamese: `"Vô địch tuyệt đối"` for open weight categories
