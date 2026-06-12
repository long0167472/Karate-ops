---
name: match-entities
description: Entity schema for Match, KumiteMatchState, MatchParticipant, KataVote, MatchScoreEvent, MatchAuditEvent, Tatami
type: data-model
version: "1.0"
last_updated: "2026-06-11"
metadata:
  owner: backend
source_references:
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/entity/Match.java }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/entity/KumiteMatchState.java }
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

**MatchStatus**: `SCHEDULED, READY, RUNNING, PAUSED, REVIEW, HANTEI, VOTING, COMPLETED, LOCKED, CANCELLED`
**WinType**: `IPPON, SHIDO, HANTEI, KIKEN, SHIKKAKU, FUSEN, HANSOKU, DISQUALIFIED, MANUAL, BYE`
**Side**: `AKA, AO`
**CategoryDiscipline**: `KUMITE, KATA, PARA_KATA, TEAM_KUMITE, TEAM_KATA`

**Status transition flow**:
```
SCHEDULED → READY (when 2 participants present)
READY → RUNNING (on first score/timer event)
RUNNING → PAUSED (TIMER_STOP)
RUNNING → HANTEI (tie detected by KumiteRuleEngine)
RUNNING → VOTING (kata vote submitted)
Any → LOCKED (confirmResult() called)
```

**Known issue**: No guard on STATUS_CHANGE event — any status can be forced. See [known-issues/patterns.md Pattern 2].

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
| `akaSenshu` | boolean | No | false | AKA first-point flag |
| `aoSenshu` | boolean | No | false | AO first-point flag |
| `akaChui` | int | No | 0 | Warning count (0–3) |
| `aoChui` | int | No | 0 | Warning count (0–3) |
| `akaHansokuChui` | boolean | No | false | Serious warning flag |
| `aoHansokuChui` | boolean | No | false | |
| `akaHansoku` | boolean | No | false | Disqualification flag |
| `aoHansoku` | boolean | No | false | |
| `akaShikkaku` | boolean | No | false | Expulsion flag |
| `aoShikkaku` | boolean | No | false | |
| `akaKiken` | boolean | No | false | Withdrawal flag |
| `aoKiken` | boolean | No | false | |
| `durationMs` | int | No | 180000 | Configured match duration in ms (3 min) |
| `remainingMs` | int | No | 180000 | Time left; decremented on TIMER_STOP |
| `timerRunning` | boolean | No | false | |
| `timerStartedAt` | Instant | Yes | — | Cleared on TIMER_STOP |
| `updatedAt` | Instant | No | — | @PrePersist @PreUpdate |

Created by draw algorithm for every KUMITE/TEAM_KUMITE match. Not created for KATA matches.

---

## MatchParticipant

**Table**: `match_participants`

| Field | Type | Nullable | Notes |
|-------|------|----------|-------|
| `match` | Match | No | FK EAGER |
| `entry` | Entry | No | FK EAGER |
| `side` | Side | No | AKA or AO |
| `team` | [NEEDS HUMAN INPUT] | — | Team entity if TEAM_KUMITE |

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
| `type` | ScoreEventType | No | `SCORE_DELTA, PENALTY, SENSHU, TIMER_START, TIMER_STOP, TIMER_SET, KATA_VOTE, STATUS_CHANGE, RESULT_CONFIRMED` |
| `side` | Side | Yes | AKA or AO |
| `points` | Integer | Yes | For SCORE_DELTA |
| `penaltyCode` | String | Yes | For PENALTY |
| `judgeNumber` | Integer | Yes | For KATA_VOTE |
| `voteSide` | Side | Yes | For KATA_VOTE |
| `payloadJson` | String | Yes | Extra arbitrary payload |
| `occurredAt` | Instant | No | Event timestamp |

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
`GET /api/tatamis/{id}/current-match` returns the current RUNNING match on this tatami (204 if none).

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
