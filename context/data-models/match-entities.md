---
name: match-entities
description: Entity schema for Match, KumiteMatchState, MatchParticipant, KataVote, MatchScoreEvent, MatchAuditEvent, Tatami
type: data-model
version: "1.2"
last_updated: "2026-07-01"
metadata:
  owner: backend
source_references:
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/entity/Match.java }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/entity/KumiteMatchState.java }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/service/TeamKumiteResolutionService.java }
knowledge_graph_refs:
  - { community: "tournament-operations", hub_node: "Match" }
related_context_files:
  - context/features/tatami-match-scoring.md
  - context/features/draw-brackets.md
  - context/data-models/tournament-entities.md
---

# Data Model: Match Entities

## Match

**Table**: `matches`

| Field | Type | Nullable | Default | Notes |
|-------|------|----------|---------|-------|
| `tournament` | Tournament | No | — | FK EAGER |
| `category` | Category | No | — | FK EAGER |
| `bracket` | Bracket | Yes | — | FK EAGER; null for exhibition matches |
| `tatami` | Tatami | Yes | — | FK EAGER; assigned before match runs |
| `matchNumber` | Integer | No | — | Sequential within tournament |
| `roundName` | String(120) | No | — | "Final", "Semifinal", "Quarterfinal", "Bronze Medal" |
| `roundNumber` | Integer | No | — | 1=first round, higher=later |
| `bracketPosition` | Integer | No | 1 | Position within round |
| `status` | MatchStatus | No | `SCHEDULED` | |
| `scheduledAt` | Instant | Yes | — | Optional scheduled time |
| `mode` | CategoryDiscipline | No | `KUMITE` | Determines if KumiteMatchState is created |
| `winnerEntry` | Entry | Yes | — | FK EAGER; set after confirmResult() |
| `winnerAthlete` | Athlete | Yes | — | FK EAGER; set after confirmResult() |
| `winType` | WinType | Yes | — | How the match was won |
| `winnerNextMatch` | Match | Yes | — | FK EAGER; self-referencing — where winner goes |
| `winnerNextSide` | Side | Yes | — | AKA or AO in the next match |
| `loserNextMatch` | Match | Yes | — | FK EAGER; repechage bronze destination |
| `loserNextSide` | Side | Yes | — | |
| `teamMatchGroupId` | UUID | Yes | — | Groups multiple TEAM_KUMITE bout matches into one aggregate team match |
| `teamBoutOrder` | Integer | Yes | — | Bout order within `teamMatchGroupId`; extra bout uses the next order |
| `teamExtraBout` | boolean | No | false | True for Article 12.5 extra bout / tie-break bout |

**MatchStatus**: `SCHEDULED, READY, RUNNING, PAUSED, REVIEW, HANTEI, HIKIWAKE, RESULT_PENDING_CONFIRMATION, VOTING, COMPLETED, LOCKED, CANCELLED`
**WinType**: `POINTS, EIGHT_POINT_LEAD, TIME_UP, SENSHU, HANTEI, HIKIWAKE, KIKEN, HANSOKU, SHIKKAKU, TEN_SECOND_RULE, DISQUALIFICATION, KATA_VOTES, BYE, MANUAL`
**Side**: `AKA, AO`
**CategoryDiscipline**: `KUMITE, KATA, PARA_KATA, TEAM_KUMITE, TEAM_KATA`

**Status transition flow**:
```
SCHEDULED → READY (when 2 participants are present)
READY/PAUSED → RUNNING (timer start or first live action)
RUNNING/PAUSED/REVIEW → HANTEI (time-up tie with no senshu holder)
RUNNING/PAUSED/REVIEW → RESULT_PENDING_CONFIRMATION (backend terminal decision exists)
HANTEI/RESULT_PENDING_CONFIRMATION → REVIEW (explicit reopen)
Any live state → LOCKED (confirmResult())
```

**Guardrail note**: `STATUS_CHANGE` is now bounded by an explicit transition matrix. `HANTEI`, `HIKIWAKE`, and `RESULT_PENDING_CONFIRMATION` are frozen states, not generic live statuses.

`HIKIWAKE` is used for round-robin / pool and team regular bouts that remain tied after score, `SENSHU`, IPPON, and WAZA-ARI tie-breakers. Individual elimination and team extra bouts still use `HANTEI` for the same unresolved time-up state.

**TEAM_KUMITE aggregate model**: There is no active `Team` JPA entity in the current backend source. Team category entries are represented by `Entry.teamId`, `Entry.teamName`, and `Entry.teamMemberAthleteIds`; `MatchParticipant.entry` points at that team entry. `Match.teamMatchGroupId` is the aggregate boundary for multiple regular bout `Match` rows belonging to one team-vs-team match. `TeamKumiteResolutionService` reads all matches in the group and applies WKF 2026 Article 12.5 after each grouped bout is locked:
- more regular bout victories wins the aggregate
- tied bout victories fall back to regular-bout total points
- tied victories and points require an extra bout; backend creates a `TEAM_KUMITE` match with `teamExtraBout=true`
- the extra bout resolves by normal Kumite logic, including `HANTEI` when score / `SENSHU` cannot separate the sides

For grouped TEAM_KUMITE bouts, individual bout confirmation does not advance the bracket by itself. Advancement, tournament points, and medals run once when the aggregate resolves.

---

## KumiteMatchState

**Table**: `kumite_match_state`

**Special**: Uses `@MapsId` — `match_id` is BOTH the foreign key AND the primary key. 1:1 with Match.

| Field | Type | Nullable | Default | Notes |
|-------|------|----------|---------|-------|
| `matchId` | UUID | No | — | @Id; @MapsId from `match` field |
| `match` | Match | No | — | @OneToOne @MapsId @JoinColumn(match_id) |
| `akaScore` | int | No | 0 | Red corner score |
| `aoScore` | int | No | 0 | Blue corner score |
| `akaSenshu` | boolean | No | false | Legacy compatibility flag, derived from `senshuHolder` |
| `aoSenshu` | boolean | No | false | Legacy compatibility flag, derived from `senshuHolder` |
| `akaChui` | int | No | 0 | Legacy compatibility value derived from canonical penalty ladders |
| `aoChui` | int | No | 0 | Legacy compatibility value derived from canonical penalty ladders |
| `akaHansokuChui` | boolean | No | false | Legacy compatibility flag derived from canonical penalty ladders |
| `aoHansokuChui` | boolean | No | false | |
| `akaHansoku` | boolean | No | false | Direct terminal flag or derived ladder terminal |
| `aoHansoku` | boolean | No | false | |
| `akaShikkaku` | boolean | No | false | Direct expulsion flag |
| `aoShikkaku` | boolean | No | false | |
| `akaKiken` | boolean | No | false | Direct withdrawal flag |
| `aoKiken` | boolean | No | false | |
| `akaPenaltyLevel` | KumitePenaltyLevel | No | `NONE` | Canonical WKF 2026 single penalty ladder |
| `aoPenaltyLevel` | KumitePenaltyLevel | No | `NONE` | |
| `akaPenaltyReasonCode` | String(80) | Yes | — | Controlled reason code for the latest ladder state |
| `aoPenaltyReasonCode` | String(80) | Yes | — | |
| `akaCategory1Penalty` | KumitePenaltyLevel | No | `NONE` | Legacy compatibility value derived from `akaPenaltyLevel` |
| `aoCategory1Penalty` | KumitePenaltyLevel | No | `NONE` | |
| `akaCategory2Penalty` | KumitePenaltyLevel | No | `NONE` | Legacy compatibility value, always `NONE` for canonical WKF 2026 flow |
| `aoCategory2Penalty` | KumitePenaltyLevel | No | `NONE` | |
| `senshuHolder` | Side | Yes | — | Canonical `SENSHU` holder |
| `senshuAwardedAt` | Instant | Yes | — | |
| `senshuRevoked` | boolean | No | false | |
| `senshuRevokedAt` | Instant | Yes | — | |
| `senshuRevocationReasonCode` | String(80) | Yes | — | |
| `senshuReawardBlocked` | boolean | No | false | Set when senshu is revoked in the last 15 seconds |
| `videoReviewStatus` | VideoReviewStatus | No | `IDLE` | One active VR workflow at a time |
| `videoReviewActiveSide` | Side | Yes | — | Requesting side while VR is open |
| `akaVideoReviewCardAvailable` | boolean | No | true | |
| `aoVideoReviewCardAvailable` | boolean | No | true | |
| `videoReviewLastResolution` | VideoReviewResolution | Yes | — | |
| `medicalStatus` | MedicalStatus | No | `IDLE` | |
| `medicalInjuredSide` | Side | Yes | — | |
| `medicalStartedAt` | Instant | Yes | — | |
| `medicalDeadlineAt` | Instant | Yes | — | Backend-owned countdown deadline |
| `medicalLastOutcome` | MedicalOutcome | Yes | — | |
| `decisionWinnerSide` | Side | Yes | — | Backend-owned winner decision |
| `decisionWinType` | WinType | Yes | — | |
| `decisionReasonCode` | String(80) | Yes | — | |
| `decisionReasonText` | String(255) | Yes | — | |
| `decisionFrozen` | boolean | No | false | |
| `decisionConfirmable` | boolean | No | false | |
| `lastLiveStatus` | MatchStatus | Yes | — | Last resumable live status before `REVIEW` / freeze |
| `durationMs` | int | No | 180000 | Configured match duration in ms (3 min) |
| `remainingMs` | int | No | 180000 | Time left; decremented on TIMER_STOP |
| `timerRunning` | boolean | No | false | |
| `timerStartedAt` | Instant | Yes | — | Cleared on TIMER_STOP |
| `updatedAt` | Instant | No | — | @PrePersist @PreUpdate |

Created by draw algorithm for every KUMITE/TEAM_KUMITE match. Not created for KATA matches.

For `WinType.HIKIWAKE`, `decisionWinnerSide`, `Match.winnerEntry`, and `Match.winnerAthlete` remain null. The match is still locked after confirmation, but winner/loser bracket advancement and winner-based points/medals are skipped.

**Canonical enums introduced for kumite flow**
- `KumitePenaltyLevel`: `NONE, CHUI_1, CHUI_2, CHUI_3, HANSOKU_CHUI, HANSOKU`
- `KumitePenaltyReasonCode`: `JOGAI, MUBOBI, PASSIVITY, AVOIDING_COMBAT, EXCESSIVE_CONTACT, GRABBING, WAKARETE_VIOLATION, REFEREE_ORDER_VIOLATION`
- `KumitePenaltyCategory`: `CATEGORY_1, CATEGORY_2` (legacy request/response compatibility only)
- `VideoReviewStatus`: `IDLE, REQUESTED`
- `VideoReviewResolution`: `DENIED, AWARD_SCORE, TORIMASEN, REVOKE_SENSHU, MIENAI, TECHNICAL_PROBLEM`
- `MedicalStatus`: `IDLE, ACTIVE`
- `MedicalOutcome`: `FIT_TO_CONTINUE, UNFIT_TEN_SECOND_RULE, CANCELLED`

---

## MatchParticipant

**Table**: `match_participants`

| Field | Type | Nullable | Notes |
|-------|------|----------|-------|
| `match` | Match | No | FK EAGER |
| `entry` | Entry | No | FK EAGER |
| `side` | Side | No | AKA or AO |
| `team` | Derived from Entry | — | No active Team entity; TEAM_KUMITE uses `entry.teamId` / `teamName` |

Unique constraint: `(match_id, side)` — one participant per side per match.
Created by draw algorithm; updated by `advanceWinner()` / `advanceLoser()` after confirmResult.

---

## KataVote

**Table**: `kata_votes`

| Field | Type | Nullable | Notes |
|-------|------|----------|-------|
| `match` | Match | No | FK |
| `judgeNumber` | Integer | No | Seat number of the judge (1..5 or 1..7) |
| `side` | Side | No | Which side (AKA/AO) this judge voted for |

Unique: `(match_id, judgeNumber)` — judge can vote once per match (update replaces).
Created/updated via `KATA_VOTE` event type.

---

## MatchScoreEvent

**Table**: `match_score_events`

Event log for every scoring action. Append-only — never soft-deleted.

| Field | Type | Nullable | Notes |
|-------|------|----------|-------|
| `match` | Match | No | FK |
| `actorUserId` | UUID | Yes | Who performed the action |
| `type` | ScoreEventType | No | `TIMER_START, TIMER_STOP, TIMER_SET, SCORE_DELTA, SCORE_EXCHANGE, PENALTY, SENSHU, SENSHU_REVOKE, VIDEO_REVIEW_REQUEST, VIDEO_REVIEW_RESOLVE, MEDICAL_START, MEDICAL_RESOLVE, HANTEI_DECISION, VR, KATA_VOTE, KATA_REVEAL, STATUS_CHANGE, RESULT_CONFIRMED, MANUAL_CORRECTION` |
| `side` | Side | Yes | AKA or AO |
| `points` | Integer | Yes | For SCORE_DELTA |
| `penaltyCode` | String | Yes | Legacy PENALTY compatibility only |
| `judgeNumber` | Integer | Yes | For KATA_VOTE |
| `voteSide` | Side | Yes | For KATA_VOTE |
| `payloadJson` | String | Yes | Extra payload / audit detail for events that need it. For `SCORE_EXCHANGE`, stores atomic exchange deltas such as `akaPoints`, `aoPoints`, and `exchangeId`. For `VIDEO_REVIEW_REQUEST` / `VIDEO_REVIEW_RESOLVE`, stores backend-structured audit metadata including request side, resolution, card kept/consumed, and optional reason fields. |
| `occurredAt` | Instant | No | Event timestamp |

Penalty / VR / medical explicit fields now live in `MatchEventRequest` and are copied into BE state transitions; `payloadJson` is no longer the primary request contract for those workflows, but is exposed in recent events for audit.

---

## MatchAuditEvent

**Table**: `match_audit_events`

| Field | Type | Nullable | Notes |
|-------|------|----------|-------|
| `match` | Match | No | FK |
| `actorUserId` | UUID | Yes | |
| `action` | String | No | e.g., `"RESULT_CONFIRMED"` |
| `reason` | String | Yes | From ConfirmResultRequest.reason |
| `occurredAt` | Instant | No | |

---

## Tatami

**Table**: `tatamis`

| Field | Type | Nullable | Default | Notes |
|-------|------|----------|---------|-------|
| `tournament` | Tournament | No | — | FK EAGER |
| `tatamiNo` | Integer | No | — | @Positive; 1-based number |
| `name` | String | No | — | Display name |
| `status` | TatamiStatus | No | — | `ACTIVE, INACTIVE` |

Unique constraint: `(tournament_id, tatami_no)`.
`GET /api/tatamis/{id}/current-match` returns the current live tatami match. After result confirmation, `tatami.currentMatch` is cleared and the endpoint returns `204` until the next ready/live match is promoted.

---

## Relationships

```
Tournament
  └── Tatami (1 per mat area)
  └── Category
        └── Bracket
              └── Match (self-ref: winnerNextMatch, loserNextMatch)
                    ├── MatchParticipant (AKA, AO) → Entry → Athlete
                    ├── KumiteMatchState (@MapsId = match.id)
                    ├── KataVote (per judge)
                    ├── MatchScoreEvent (append-only log)
                    └── MatchAuditEvent (result confirmations)
```
