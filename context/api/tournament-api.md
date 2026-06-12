---
name: tournament-api
description: Complete endpoint reference for tournaments, categories, entries, tatamis, matches, and draw
type: api
version: "1.0"
last_updated: "2026-06-11"
metadata:
  owner: backend
  base_path: /api
source_references:
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/web/TournamentController.java }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/web/CategoryController.java }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/web/TatamiController.java }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/web/MatchController.java }
knowledge_graph_refs:
  - { community: "tournament-operations", hub_node: "Tournament" }
related_context_files:
  - context/features/tournament-operations.md
  - context/features/draw-brackets.md
  - context/features/tatami-match-scoring.md
  - context/api/match-api.md
---

# API Reference: Tournament Management

All responses wrapped in `ApiResponse<T>`. Auth: `Authorization: Bearer <jwt>` required.

## Tournaments (`/api/tournaments`)

| Method | Path | Body | Response | Status |
|--------|------|------|----------|--------|
| GET | `/api/tournaments` | — | `List<TournamentResponse>` | 200 |
| GET | `/api/tournaments/{id}` | — | `TournamentResponse` | 200 |
| POST | `/api/tournaments` | `TournamentCreateRequest` | `TournamentResponse` | 201 |
| PATCH | `/api/tournaments/{id}` | `TournamentUpdateRequest` | `TournamentResponse` | 200 |
| DELETE | `/api/tournaments/{id}` | — | — | 204 |

**TournamentCreateRequest / TournamentUpdateRequest** (same fields):
```
@NotBlank name, code, description, location,
LocalDate startsOn, LocalDate endsOn,
UUID ownerOrganizationId,
TournamentVisibility visibility,    // PUBLIC | PRIVATE | INVITE_ONLY
TournamentStatus status,            // DRAFT | REGISTRATION_OPEN | ... | ARCHIVED
RulesetVersion rulesetVersion,      // WKF_2026
String organizerName,
Integer tatamiCount,                // ≥ 1
List<String> competitionLevels,    // ["PHONG_TRAO","NANG_CAO"] stored as CSV
RulesetPreset rulesetPreset,       // WKF
String ruleSnapshotJson
```

**Service defaults on create**: visibility=PRIVATE, status=DRAFT, rulesetVersion=WKF_2026, tatamiCount=1, competitionLevels=["PHONG_TRAO","NANG_CAO"], rulesetPreset=WKF

**TournamentStatus values**: `DRAFT, REGISTRATION_OPEN, REGISTRATION_CLOSED, DRAWING, RUNNING, COMPLETED, ARCHIVED`

---

## Tournament Delegations (`/api/tournaments/{id}/participants`)

| Method | Path | Body | Response | Status |
|--------|------|------|----------|--------|
| GET | `/api/tournaments/{id}/participants` | — | `List<TournamentParticipantResponse>` | 200 |
| POST | `/api/tournaments/{id}/participants` | `ParticipantCreateRequest` | `TournamentParticipantResponse` | 201 |
| PATCH | `/api/tournaments/{id}/participants/{participantId}/status` | `ParticipantStatusRequest` | `TournamentParticipantResponse` | 200 |

**ParticipantCreateRequest**: `{ @NotNull UUID organizationId, String displayName, ParticipantStatus status }`
**ParticipantStatusRequest**: `{ @NotNull ParticipantStatus status }`
**ParticipantStatus**: `INVITED, REQUESTED, APPROVED, REJECTED, WITHDRAWN`

Business rule: only `APPROVED` delegations can register entries.

---

## Categories (`/api`)

| Method | Path | Body | Response | Status |
|--------|------|------|----------|--------|
| GET | `/api/tournaments/{tournamentId}/categories` | — | `List<CategoryResponse>` | 200 |
| POST | `/api/tournaments/{tournamentId}/categories` | `CategoryCreateRequest` | `CategoryResponse` | 201 |
| GET | `/api/categories/{id}` | — | `CategoryResponse` | 200 |
| PATCH | `/api/categories/{id}` | `CategoryUpdateRequest` | `CategoryResponse` | 200 |
| DELETE | `/api/categories/{id}` | — | — | 204 |

**CategoryCreateRequest / CategoryUpdateRequest** (same fields):
```
@NotBlank name, @NotNull CategoryDiscipline discipline,
PersonGender gender,      // MALE | FEMALE | OPEN | MIXED (default OPEN)
Integer ageMin, Integer ageMax,
BigDecimal weightMinKg, BigDecimal weightMaxKg,
CompetitionLevel competitionLevel,  // default OPEN
String weightLabel,      // auto-generated if not provided
Boolean openWeight,      // auto-set if kumite + no weight bounds
EntryType entryType,     // INDIVIDUAL | TEAM (default INDIVIDUAL)
String status,           // "DRAFT" | "DRAWN"
RulesetVersion rulesetVersion,
Boolean repechageEnabled,            // default true
Integer matchDurationSeconds,        // default 180, min 30
Integer kataJudgeCount,              // must be 5 or 7
Boolean kataRepeatAllowed,
Integer entryLimitPerOrganization
```

**CategoryDiscipline**: `KUMITE, KATA, PARA_KATA, TEAM_KUMITE, TEAM_KATA`

---

## Entries (`/api/categories/{id}/entries`)

| Method | Path | Body | Response | Status |
|--------|------|------|----------|--------|
| GET | `/api/categories/{id}/entries` | — | `List<EntryResponse>` | 200 |
| POST | `/api/categories/{id}/entries` | `EntryCreateRequest` | `EntryResponse` | 201 |
| DELETE | `/api/categories/{id}/entries/{entryId}` | — | — | 204 |

**EntryCreateRequest**:
```
@NotNull UUID tournamentParticipantId,
UUID athleteId,              // for INDIVIDUAL
UUID teamId, String teamName, List<UUID> teamMemberAthleteIds,  // for TEAM
Integer seedNo,
EntryStatus status,
BigDecimal registrationWeightKg
```

**EntryStatus**: `REGISTERED, CHECKED_IN, WITHDRAWN, DISQUALIFIED`
**WeighInStatus** (computed): `VALID, MISSING_WEIGHT, OUT_OF_CLASS, NEEDS_ORGANIZER_REVIEW`

Validation: APPROVED delegation required; athlete on ClubRoster; no duplicate entry; age/gender/weight checked.

---

## Draw (`/api/categories/{id}/draw`)

| Method | Path | Body | Response | Status |
|--------|------|------|----------|--------|
| POST | `/api/categories/{id}/draw` | `DrawRequest` | `DrawResponse` | 201 |

**DrawRequest**:
```
BracketType bracketType,   // SINGLE_ELIMINATION | REPECHAGE | ROUND_ROBIN | POOL
Boolean shuffle,            // randomize entry order (default false = by seedNo)
Boolean enableRepechage     // override category.repechageEnabled
```

**DrawResponse**: `{ bracketId, bracketSize, entryCount, matches[] }`

See [draw-brackets.md](../features/draw-brackets.md) for full algorithm.

---

## Tatamis (`/api`)

| Method | Path | Body | Response | Status |
|--------|------|------|----------|--------|
| GET | `/api/tournaments/{tournamentId}/tatamis` | — | `List<TatamiResponse>` | 200 |
| POST | `/api/tournaments/{tournamentId}/tatamis` | `TatamiCreateRequest` | `TatamiResponse` | 201 |
| PATCH | `/api/tatamis/{id}` | `TatamiUpdateRequest` | `TatamiResponse` | 200 |
| DELETE | `/api/tatamis/{id}` | — | — | 204 |
| GET | `/api/tatamis/{id}/current-match` | — | `MatchResponse` | 200 or 204 (empty) |
| POST | `/api/tatamis/{id}/assign-match` | `{ @NotNull UUID matchId }` | `MatchResponse` | 200 |

**TatamiCreateRequest**: `{ @NotNull @Positive Integer tatamiNo, @NotBlank String name, TatamiStatus status }`
**TatamiStatus**: [NEEDS HUMAN INPUT — enum values not confirmed]

`GET /tatamis/{id}/current-match` returns `ResponseEntity<MatchResponse>` — returns 204 No Content if no match currently running on that tatami.

---

## Matches (`/api`)

| Method | Path | Body | Response | Status |
|--------|------|------|----------|--------|
| GET | `/api/tournaments/{tournamentId}/matches` | — | `List<MatchResponse>` | 200 |
| GET | `/api/matches/{id}` | — | `MatchResponse` | 200 |
| POST | `/api/matches/{id}/events` | `MatchEventRequest` | `MatchResponse` | 200 |
| POST | `/api/matches/{id}/result` | `ConfirmResultRequest` | `MatchResponse` | 200 |

See [match-api.md](./match-api.md) for full match event and result request formats.

---

## Known Inconsistencies

- Tournament list returns ALL tournaments regardless of visibility — no filter by `visibility=PUBLIC`; access control enforcement TBD
- No `PUT /api/tournaments/{id}/status` endpoint — status changes via PATCH on the full tournament record
- `tatamiCount` on Tournament is informational; actual tatami entities must still be created via `POST /api/tournaments/{id}/tatamis`
- `GET /matches/{id}` checks tournament view permission per match — might be expensive for list views
