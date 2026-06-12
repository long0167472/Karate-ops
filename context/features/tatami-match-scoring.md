---
name: tatami-match-scoring
description: Live match control — event recording, scoring, timer, penalty, kata voting, result confirmation, and realtime broadcast
type: feature
version: "1.0"
last_updated: "2026-06-11"
criticality: CRITICAL
metadata:
  owner: backend
  affected_services: [karate-tournament-backend, karate-ops-fe]
  affected_domains: [tournaments, matches]
source_references:
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/service/impl/MatchServiceImpl.java, note: "All match business logic" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/web/MatchController.java, note: "Match endpoints" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/entity/KumiteMatchState.java, note: "Score/timer state" }
  - { file_path: karate-ops-fe/src/useScoreboard.ts, note: "FE hook: REST fetch + STOMP subscription" }
  - { file_path: karate-ops-fe/src/App.tsx, note: "ControlPage, DisplayPage, JudgePage, OverlayPage" }
knowledge_graph_refs:
  - { community: "tournament-operations", hub_node: "Match" }
related_context_files:
  - context/features/draw-brackets.md
  - context/data-models/match-entities.md
  - context/governance/api-standards.md
---

# Feature: Tatami Match Scoring

## Actors
- **Control desk (TATAMI_OPERATOR)** — controls timer, records scores, confirms result
- **Judge (JUDGE)** — submits kata votes
- **Display / Overlay** — read-only consumers of realtime state
- **RealtimePublisher** — pushes match state after every mutation

## Tatami Views (Frontend)

| Route | Component | Role |
|-------|-----------|------|
| `/control?tatamiId=&tournamentId=` | `ControlPage` | Full control |
| `/display?tatamiId=&tournamentId=` | `DisplayPage` | Scoreboard display |
| `/judge?tatamiId=&tournamentId=` | `JudgePage` | Kata judge voting |
| `/overlay?...` | `OverlayPage` | OBS/stream overlay |

All views share state via `useScoreboard(tatamiId, tournamentId)`:
1. `GET /api/tatamis/{tatamiId}/current-match` → load initial state
2. Subscribe to STOMP `/topic/tatamis/{tatamiId}` → receive live `MatchResponse` updates

Free tatami mode (no `tatamiId`): `useManualTatami` — local state only, no auth required.

## REST Endpoints (MatchController)

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/api/tournaments/{tournamentId}/matches` | List all matches in tournament |
| `GET` | `/api/matches/{id}` | Get single match state |
| `POST` | `/api/matches/{id}/events` | Record a scoring/control event |
| `POST` | `/api/matches/{id}/result` | Confirm final result |

## Flow 1: Recording a Scoring Event

**Trigger**: `POST /api/matches/{id}/events` with `MatchEventRequest`

```java
public record MatchEventRequest(
    ScoreEventType type,    // required
    Side side,              // required for score/penalty/senshu
    Integer points,         // for SCORE_DELTA
    String penaltyCode,     // for PENALTY
    Integer judgeNumber,    // for KATA_VOTE (1..5 or 1..7)
    Side voteSide,          // for KATA_VOTE (AKA or AO)
    Long timerMs,           // for TIMER_SET
    String payloadJson      // optional extra payload
)
```

**ScoreEventType values**: `SCORE_DELTA, PENALTY, SENSHU, TIMER_START, TIMER_STOP, TIMER_SET, KATA_VOTE, STATUS_CHANGE, RESULT_CONFIRMED`

**Dispatch table** in `MatchServiceImpl.applyEvent()`:

| Event Type | Handler | Side Effects |
|-----------|---------|--------------|
| `SCORE_DELTA` | `applyScoreDelta()` | Updates akaScore/aoScore; SCHEDULED→RUNNING; calls `applyKumiteSuggestion()` |
| `PENALTY` | `applyPenalty()` | Updates penalty flags; calls `applyKumiteSuggestion()` |
| `SENSHU` | `applySenshu()` | Sets akaSenshu or aoSenshu |
| `TIMER_START` | `applyTimerStart()` | Sets timerRunning=true; RUNNING state |
| `TIMER_STOP` | `applyTimerStop()` | Decrements remainingMs; RUNNING→PAUSED |
| `TIMER_SET` | `applyTimerSet()` | Sets remainingMs; calls `applyKumiteSuggestion()` |
| `KATA_VOTE` | `applyKataVote()` | Creates/updates KataVote; status→VOTING |
| `STATUS_CHANGE` | direct | Sets match.status from request |

After any event: saves `MatchScoreEvent` record + calls `RealtimePublisher.publishMatch()`.

## Flow 2: Scoring Rules

### Score Delta
- `points` must be in `{-3, -2, -1, 1, 2, 3}` — other values → `BadRequestException`
- Score clamped to minimum 0: `Math.max(0, state.akaScore + points)`
- Points meaning: `+1` = yuko, `+2` = waza-ari, `+3` = ippon, negatives = undo score

### Penalties (PENALTY event)
`penaltyCode` normalized to uppercase. Supported codes:

| Code | Field affected | Cap |
|------|---------------|-----|
| `CHUI` | `akaChui` / `aoChui` | Max 3 |
| `HANSOKU_CHUI` | `akaHansokuChui` / `aoHansokuChui` | boolean |
| `HANSOKU` | `akaHansoku` / `aoHansoku` | boolean |
| `SHIKKAKU` | `akaShikkaku` / `aoShikkaku` | boolean |
| `KIKEN` | `akaKiken` / `aoKiken` | boolean |

Any other code → `BadRequestException("Unsupported penalty code: ...")`

### Timer
- `durationMs` default: 180,000ms (3 minutes)
- `remainingMs` starts at durationMs; `TIMER_STOP` calculates elapsed via `timerStartedAt` and decrements
- `timerRunning = true` + `timerStartedAt = Instant.now()` on START
- Remaining clamped to 0 — never negative
- FE is responsible for counting down from remainingMs; BE is authoritative on each event

### Kumite Suggestion (Auto-detect winner)
After each SCORE_DELTA, PENALTY, or TIMER_SET, `applyKumiteSuggestion()` is called:
- Passes `KumiteSnapshot` (all score/penalty state) to `KumiteRuleEngine.suggestWinner()`
- If suggestion is null AND win condition is HANTEI → sets `match.status = HANTEI`

## Flow 3: Confirm Result

**Trigger**: `POST /api/matches/{id}/result`

```java
public record ConfirmResultRequest(
    Side winnerSide,    // required
    WinType winType,    // optional (default MANUAL)
    String reason       // optional audit note
)
```

**Steps in `MatchServiceImpl.confirmResult()`**:
1. Load match + validate participant on `winnerSide` exists (else `BusinessConflictException`)
2. Set `match.winnerEntry`, `match.winnerAthlete`, `match.winType` (default MANUAL)
3. Set `match.status = LOCKED`
4. `saveResultEvent()` — saves MatchScoreEvent(type=RESULT_CONFIRMED) + MatchAuditEvent(action="RESULT_CONFIRMED", reason)
5. `advanceWinner()` — creates MatchParticipant in `match.winnerNextMatch`; transitions that match SCHEDULED→READY when 2 participants present
6. `advanceLoser()` — same for loser side into `match.loserNextMatch` (repechage bronze)
7. `recordMedalsIfReady()`:
   - If `roundName == "Final"` → GOLD for winner, SILVER for loser
   - If `roundName.startsWith("Bronze Medal")` → BRONZE for winner
8. Publish match + winnerNextMatch + loserNextMatch via `RealtimePublisher`

**WinType values**: `IPPON, SHIDO, HANTEI, KIKEN, SHIKKAKU, FUSEN, HANSOKU, DISQUALIFIED, MANUAL, BYE`

## KumiteMatchState Fields (Full Reference)

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `matchId` | UUID | — | PK, @MapsId from Match |
| `akaScore` | int | 0 | AKA point total |
| `aoScore` | int | 0 | AO point total |
| `akaSenshu` | boolean | false | AKA has senshu (first point) |
| `aoSenshu` | boolean | false | AO has senshu |
| `akaChui` | int | 0 | AKA chui count (0–3) |
| `aoChui` | int | 0 | AO chui count (0–3) |
| `akaHansokuChui` | boolean | false | AKA hansoku-chui flag |
| `aoHansokuChui` | boolean | false | AO hansoku-chui flag |
| `akaHansoku` | boolean | false | AKA hansoku (disqualified) |
| `aoHansoku` | boolean | false | AO hansoku |
| `akaShikkaku` | boolean | false | AKA shikkaku (expulsion) |
| `aoShikkaku` | boolean | false | AO shikkaku |
| `akaKiken` | boolean | false | AKA kiken (withdrawal) |
| `aoKiken` | boolean | false | AO kiken |
| `durationMs` | int | 180000 | Configured match duration |
| `remainingMs` | int | 180000 | Time remaining |
| `timerRunning` | boolean | false | Timer active |
| `timerStartedAt` | Instant | null | When timer last started |

## Match Status State Machine

```
SCHEDULED ─→ READY ─→ RUNNING ─→ PAUSED
                ↘          ↘
              HANTEI      VOTING
                  ↘      ↙
                  COMPLETED / LOCKED
```

**MatchStatus full set**: `SCHEDULED, READY, RUNNING, PAUSED, REVIEW, HANTEI, VOTING, COMPLETED, LOCKED, CANCELLED`

Known issue: **No transition guard** — any status can be forced to any other via STATUS_CHANGE event. See [known-issues/patterns.md Pattern 2].

## Realtime Broadcast

After any match mutation:
```java
realtimePublisher.publishMatch(matchResponse);
// Publishes to:
//   /topic/tatamis/{tatamiId}            ← all tatami views
//   /topic/tournaments/{tournamentId}/dashboard  ← tournament dashboard
```

WebSocket endpoint: `/ws` (STOMP). FE subscribes on mount; unsubscribes on unmount.

## Edge Cases

| Scenario | Behavior |
|----------|----------|
| Event on non-kumite match for score delta | `BadRequestException` (requireKumiteState checks discipline) |
| Winner side has no participant | `BusinessConflictException("Winner side {side} has no participant")` |
| Timer START when already running | No-op (timerRunning check) |
| CHUI count > 3 | Capped at 3 (Math.min logic) |
| Score goes negative from delta | Clamped to 0 |
