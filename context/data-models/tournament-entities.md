---
name: tournament-entities
description: Entity schema for Tournament, TournamentParticipant, Category, Entry, CategoryResult, Bracket
type: data-model
version: "1.0"
last_updated: "2026-06-11"
metadata:
  owner: backend
source_references:
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/entity/Tournament.java }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/entity/Category.java }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/entity/TournamentParticipant.java }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/entity/Entry.java }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/entity/Bracket.java }
knowledge_graph_refs:
  - { community: "tournament-operations", hub_node: "Tournament" }
related_context_files:
  - context/features/tournament-operations.md
  - context/features/draw-brackets.md
  - context/data-models/match-entities.md
---

# Data Model: Tournament Entities

## Tournament

**Table**: `tournaments`

| Field | Type | Nullable | Default | Notes |
|-------|------|----------|---------|-------|
| `ownerOrganization` | Organization | Yes | — | FK EAGER; hosting club |
| `createdByUser` | AppUser | Yes | — | FK EAGER |
| `name` | String(220) | No | — | |
| `code` | String(80) | Yes | — | Optional tournament code |
| `description` | text | Yes | — | |
| `location` | String(255) | Yes | — | |
| `startsOn` | LocalDate | Yes | — | |
| `endsOn` | LocalDate | Yes | — | |
| `visibility` | TournamentVisibility | No | `PRIVATE` | |
| `status` | TournamentStatus | No | `DRAFT` | |
| `rulesetVersion` | RulesetVersion | No | `WKF_2026` | |
| `organizerName` | String(180) | Yes | — | |
| `tatamiCount` | Integer | No | 1 | Informational; tatami entities still need to be created |
| `competitionLevels` | String(120) | No | `"PHONG_TRAO,NANG_CAO"` | Stored as CSV |
| `rulesetPreset` | RulesetPreset | No | `WKF` | |
| `ruleSnapshotJson` | text | Yes | — | JSON snapshot of rules at creation |

**TournamentStatus**: `DRAFT, REGISTRATION_OPEN, REGISTRATION_CLOSED, DRAWING, RUNNING, COMPLETED, ARCHIVED`
**TournamentVisibility**: `PUBLIC, PRIVATE, INVITE_ONLY`
**RulesetVersion**: `WKF_2026, LOCAL`
**RulesetPreset**: `WKF, PHONG_TRAO, NANG_CAO, CUSTOM`

---

## TournamentParticipant

**Table**: `tournament_participants`

| Field | Type | Nullable | Default | Notes |
|-------|------|----------|---------|-------|
| `tournament` | Tournament | No | — | FK EAGER |
| `organization` | Organization | No | — | FK EAGER; participating club |
| `displayName` | String(180) | No | — | Defaults to org.name |
| `status` | ParticipantStatus | No | `REQUESTED` | |
| `approvedAt` | Instant | Yes | — | Set when status → APPROVED |

**ParticipantStatus**: `INVITED, REQUESTED, APPROVED, REJECTED, WITHDRAWN`

Unique constraint: `(tournament_id, organization_id)`. Only APPROVED delegations can add entries.

---

## Category

**Table**: `categories`

| Field | Type | Nullable | Default | Notes |
|-------|------|----------|---------|-------|
| `tournament` | Tournament | No | — | FK EAGER |
| `name` | String(180) | No | — | |
| `discipline` | CategoryDiscipline | No | `KUMITE` | |
| `gender` | PersonGender | No | `OPEN` | Filter for athlete eligibility |
| `ageMin` | Integer | Yes | — | Age validation (years) |
| `ageMax` | Integer | Yes | — | |
| `weightMinKg` | BigDecimal(6,2) | Yes | — | |
| `weightMaxKg` | BigDecimal(6,2) | Yes | — | |
| `competitionLevel` | CompetitionLevel | No | `OPEN` | |
| `weightLabel` | String(40) | Yes | — | Auto-generated: "-60kg", "+84kg", "Vô địch tuyệt đối" |
| `openWeight` | Boolean | No | false | Auto-set if kumite + no weight bounds |
| `entryType` | EntryType | No | `INDIVIDUAL` | |
| `status` | String(40) | No | `"DRAFT"` | `"DRAFT"` or `"DRAWN"` |
| `rulesetVersion` | RulesetVersion | No | `WKF_2026` | |
| `repechageEnabled` | Boolean | No | true | |
| `matchDurationSeconds` | Integer | No | 180 | Min 30; multiplied ×1000 for KumiteMatchState.durationMs |
| `kataJudgeCount` | Integer | No | 5 | Must be 5 or 7 |
| `kataRepeatAllowed` | Boolean | No | false | |
| `entryLimitPerOrganization` | Integer | Yes | — | Max entries per delegation |

**CategoryDiscipline**: `KUMITE, KATA, PARA_KATA, TEAM_KUMITE, TEAM_KATA`
**CompetitionLevel**: `PHONG_TRAO, NANG_CAO, OPEN`
**EntryType**: `INDIVIDUAL, TEAM`

Category is locked after draw: `status = "DRAWN"`. No entry changes allowed post-draw.

---

## Entry

**Table**: `entries`

| Field | Type | Nullable | Default | Notes |
|-------|------|----------|---------|-------|
| `category` | Category | No | — | FK EAGER |
| `tournamentParticipant` | TournamentParticipant | No | — | FK EAGER; must be APPROVED |
| `athlete` | Athlete | Yes | — | FK EAGER; for INDIVIDUAL |
| `teamId` | UUID | Yes | — | Team group identifier |
| `teamName` | String(180) | Yes | — | |
| `teamMemberAthleteIds` | text | Yes | — | JSON or CSV of athlete UUIDs |
| `seedNo` | Integer | Yes | — | Bracket seeding order |
| `registrationWeightKg` | BigDecimal(6,2) | Yes | — | Defaults to athlete.weightKg |
| `weighInStatus` | WeighInStatus | No | `NEEDS_ORGANIZER_REVIEW` | |
| `validationNotes` | text | Yes | — | Auto-set when weight OUT_OF_CLASS |
| `status` | EntryStatus | No | `REGISTERED` | |

**EntryStatus**: `REGISTERED, CHECKED_IN, WITHDRAWN, DISQUALIFIED`
**WeighInStatus**: `VALID, MISSING_WEIGHT, OUT_OF_CLASS, NEEDS_ORGANIZER_REVIEW`

**Weight validation**: OUT_OF_CLASS does NOT block registration. Sets `weighInStatus` and adds to `validationNotes`. See [known-issues/patterns.md Pattern 8].

---

## Bracket

**Table**: `brackets`

| Field | Type | Nullable | Default | Notes |
|-------|------|----------|---------|-------|
| `category` | Category | No | — | FK EAGER |
| `type` | BracketType | No | `REPECHAGE` | |
| `size` | Integer | No | — | nextPowerOfTwo(entryCount) |
| `status` | BracketStatus | No | `DRAFT` | |

**BracketType**: `SINGLE_ELIMINATION, REPECHAGE, ROUND_ROBIN, POOL`
**BracketStatus**: `DRAFT, GENERATED, LOCKED`

---

## CategoryResult

**Table**: `category_results`

| Field | Type | Nullable | Notes |
|-------|------|----------|-------|
| `category` | Category | No | FK EAGER |
| `entry` | Entry | No | FK EAGER |
| `placement` | Integer | No | 1=gold, 2=silver, 3=bronze |
| `medal` | String | No | `"GOLD"`, `"SILVER"`, `"BRONZE"` |

Unique constraint: `(category_id, entry_id)` — prevents duplicate medal records.
Created by `MatchServiceImpl.saveCategoryResult()` after Final and Bronze matches.

---

## Relationships

```
Tournament
  ├── Tatami (tatamiNo, used for match assignment)
  ├── TournamentParticipant (org delegation, status=APPROVED to enter)
  └── Category
        ├── Entry → TournamentParticipant + Athlete
        ├── CategoryResult → Entry (gold/silver/bronze)
        └── Bracket
              └── Match (see match-entities.md)
```
