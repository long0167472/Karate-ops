---
name: match-api
description: Match event recording, scoring commands, timer control, kata voting, and result confirmation endpoints
type: api
version: "1.0"
last_updated: "2026-06-11"
metadata:
  owner: backend
  base_path: /api/matches
source_references:
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/web/MatchController.java }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/service/impl/MatchServiceImpl.java }
knowledge_graph_refs:
  - { community: "tournament-operations", hub_node: "Match" }
related_context_files:
  - context/features/tatami-match-scoring.md
  - context/features/draw-brackets.md
  - context/governance/api-standards.md
---

# API Reference: Match Control

## Endpoints

| Method | Path | Body | Response | Status |
|--------|------|------|----------|--------|
| GET | `/api/tournaments/{tournamentId}/matches` | — | `List<MatchResponse>` | 200 |
| GET | `/api/matches/{id}` | — | `MatchResponse` | 200 |
| POST | `/api/matches/{id}/events` | `MatchEventRequest` | `MatchResponse` | 200 |
| POST | `/api/matches/{id}/result` | `ConfirmResultRequest` | `MatchResponse` | 200 |

---

## MatchEventRequest

```java
public record MatchEventRequest(
    ScoreEventType type,      // required
    Side side,                // required for SCORE_DELTA, PENALTY, SENSHU, KATA_VOTE
    Integer points,           // required for SCORE_DELTA: {-3,-2,-1,1,2,3}
    String penaltyCode,       // required for PENALTY: CHUI|HANSOKU_CHUI|HANSOKU|SHIKKAKU|KIKEN
    Integer judgeNumber,      // required for KATA_VOTE: judge seat number
    Side voteSide,            // required for KATA_VOTE
    Long timerMs,             // required for TIMER_SET: new remainingMs value
    String payloadJson        // optional: arbitrary extra payload
)
```

**Side**: `AKA` (red corner) | `AO` (blue corner)

### ScoreEventType → Required Fields

| Event | Required | Effect |
|-------|----------|--------|
| `SCORE_DELTA` | side, points | Update akaScore or aoScore; clamp to ≥0; may trigger HANTEI |
| `PENALTY` | side, penaltyCode | Update penalty flags/counters; may trigger HANTEI |
| `SENSHU` | side | Set akaSenshu or aoSenshu |
| `TIMER_START` | — | timerRunning=true; status→RUNNING |
| `TIMER_STOP` | — | Decrement remainingMs by elapsed; status RUNNING→PAUSED |
| `TIMER_SET` | timerMs | Set remainingMs to max(0, timerMs) |
| `KATA_VOTE` | side, judgeNumber, voteSide | Create/update KataVote; status→VOTING |
| `STATUS_CHANGE` | side | Force match.status to any value (no guard — see known issues) |

### Scoring Rules

**SCORE_DELTA points values and karate meaning**:
| Points | Technique |
|--------|-----------|
| `+1` | Yuko |
| `+2` | Waza-ari |
| `+3` | Ippon |
| `-1/-2/-3` | Undo (reverse) previous score |

Score never goes below 0.

**PENALTY codes**:
| Code | Field | Behavior |
|------|-------|---------|
| `CHUI` | `akaChui` / `aoChui` | Integer counter, capped at 3 |
| `HANSOKU_CHUI` | `akaHansokuChui` / `aoHansokuChui` | Boolean flag |
| `HANSOKU` | `akaHansoku` / `aoHansoku` | Boolean flag; typically auto-win for opponent |
| `SHIKKAKU` | `akaShikkaku` / `aoShikkaku` | Boolean flag; expulsion |
| `KIKEN` | `akaKiken` / `aoKiken` | Boolean flag; withdrawal |

Invalid penalty code → `BadRequestException("Unsupported penalty code: ...")`

---

## ConfirmResultRequest

```java
public record ConfirmResultRequest(
    Side winnerSide,   // required
    WinType winType,   // optional (default: MANUAL)
    String reason      // optional audit note
)
```

**WinType values**:
| Type | When used |
|------|---------|
| `IPPON` | Win by ippon |
| `SHIDO` | Win by shido count |
| `HANTEI` | Win by judge decision |
| `KIKEN` | Win by withdrawal |
| `SHIKKAKU` | Win by expulsion |
| `FUSEN` | Win by no-show/walkover |
| `HANSOKU` | Win by hansoku |
| `DISQUALIFIED` | Opponent disqualified |
| `MANUAL` | Manual override (default) |
| `BYE` | Auto-advance (set by draw algorithm) |

**What confirmResult does**:
1. Sets `match.winnerEntry`, `match.winnerAthlete`, `match.winType`, `match.status = LOCKED`
2. Saves audit event with `reason`
3. Creates `MatchParticipant` in `winnerNextMatch` (bracket advancement)
4. Creates `MatchParticipant` in `loserNextMatch` for repechage bronze
5. Saves `CategoryResult` for gold/silver/bronze (Final and Bronze Medal rounds)
6. Broadcasts to `/topic/tatamis/{tatamiId}` and `/topic/tournaments/{tournamentId}/dashboard`

---

## MatchResponse (Key Fields)

```
id, matchNumber, roundName, roundNumber, bracketPosition,
status (MatchStatus),
mode (CategoryDiscipline),
scheduledAt,
winnerEntry { id, athleteId, athleteName },
winType,
participants [
  { side, entry { id, athleteId, athleteName, seedNo }, team }
],
kumiteState {
  akaScore, aoScore,
  akaSenshu, aoSenshu,
  akaChui, aoChui,
  akaHansokuChui, aoHansokuChui,
  akaHansoku, aoHansoku,
  akaShikkaku, aoShikkaku,
  akaKiken, aoKiken,
  durationMs, remainingMs,
  timerRunning, timerStartedAt
},
tournament { id, name },
category { id, name, discipline },
tatami { id, tatamiNo, name }
```

---

## MatchStatus Values

`SCHEDULED` → `READY` → `RUNNING` ⇌ `PAUSED`
`RUNNING` → `HANTEI` (tie, judges decide)
`RUNNING` → `VOTING` (kata vote in progress)
Any → `LOCKED` (result confirmed)
Any → `ABANDONED` | `CANCELLED`

**Known issue**: No transition guard on STATUS_CHANGE event — any status can be set to any value. See [known-issues/patterns.md Pattern 2].

---

## Realtime (WebSocket)

After every `POST /events` or `POST /result`:
- Published to STOMP topic `/topic/tatamis/{tatamiId}` — full `MatchResponse`
- Published to STOMP topic `/topic/tournaments/{tournamentId}/dashboard` — dashboard snapshot

FE subscribes via `useScoreboard` hook on mount. STOMP endpoint: `ws://{host}/ws`.

---

## Error Cases

| Scenario | Exception | HTTP |
|----------|-----------|------|
| Invalid penaltyCode | `BadRequestException` | 400 |
| points not in {-3,-2,-1,1,2,3} | `BadRequestException` | 400 |
| SCORE_DELTA on non-kumite match | `BadRequestException` | 400 |
| winnerSide has no participant | `BusinessConflictException` | 409 |
| Match not found | `ResourceNotFoundException` | 404 |
| Insufficient permission | `ForbiddenException` | 403 |
