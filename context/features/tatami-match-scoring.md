---
name: tatami-match-scoring
description: Live match control - event recording, scoring, timer, penalty, kata voting, result confirmation, and realtime broadcast
type: feature
version: "1.3"
last_updated: "2026-07-01"
criticality: CRITICAL
metadata:
  owner: backend
  affected_services: [karate-tournament-backend, karate-ops-fe]
  affected_domains: [tournaments, matches]
source_references:
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/service/impl/MatchServiceImpl.java, note: "Match business logic, guards, win-type resolution" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/service/TeamKumiteResolutionService.java, note: "TEAM_KUMITE aggregate match resolution for WKF 2026 Article 12.5" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/rules/KumiteRuleEngine.java, note: "Kumite auto-suggestion and time-up tie-break" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/service/impl/DrawServiceImpl.java, note: "Single-category draw creates KumiteMatchState with category duration" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/service/impl/TournamentDrawServiceImpl.java, note: "Tournament draw creates KumiteMatchState with category duration" }
  - { file_path: karate-tournament-backend/src/main/resources/db/migration/V19__sync_kumite_match_duration.sql, note: "Backfill durationMs/remainingMs for existing kumite states" }
  - { file_path: karate-ops-fe/src/useScoreboard.ts, note: "FE hook: REST fetch + STOMP subscription" }
  - { file_path: karate-ops-fe/src/App.tsx, note: "ControlPage, DisplayPage, JudgePage, OverlayPage, manual tatami demo" }
knowledge_graph_refs:
  - { community: "tournament-operations", hub_node: "Match" }
related_context_files:
  - context/features/draw-brackets.md
  - context/data-models/match-entities.md
  - context/governance/api-standards.md
---

# Feature: Tatami Match Scoring

## Actors
- Control desk (`TATAMI_OPERATOR`) - drives live kumite flow, referee review actions, and final confirmation
- Judge (`JUDGE`) - submits kata votes only
- Display / Overlay - read-only consumers of BE-owned match state
- `RealtimePublisher` - publishes full `MatchResponse` after every accepted mutation

## Tatami Views (Frontend)

| Route | Component | Role |
|-------|-----------|------|
| `/control?tatamiId=&tournamentId=` | `ControlPage` | Live control desk |
| `/display?tatamiId=&tournamentId=` | `DisplayPage` | Tatami scoreboard |
| `/judge?tatamiId=&tournamentId=` | `JudgePage` | Kata judging only |
| `/overlay?...` | `OverlayPage` | Broadcast overlay |

All live tatami views share state through `useScoreboard(tatamiId, tournamentId)`:
1. `GET /api/tatamis/{tatamiId}/current-match` loads the current live match.
2. STOMP subscription on `/topic/tatamis/{tatamiId}` receives the full `MatchResponse` after every backend mutation.

Free tatami mode without `tatamiId` still exists in FE as a local-only demo path. Production tatami flow is BE-authoritative.

## Architecture Decision

For kumite, BE is now the single source of truth for:
- winner decision and confirmability
- `SENSHU` lifecycle
- WKF penalty ladders
- video review card state and outcomes
- medical 10-second workflow
- frozen vs live mutation rules

FE no longer synthesizes a local winner suggestion. It renders `match.kumite.decision` and sends explicit operator commands.

## Flow 1: Recording a Kumite Event

`POST /api/matches/{id}/events` persists a `MatchScoreEvent`, recomputes canonical kumite state, stops the timer when the bout becomes terminal, and publishes the updated `MatchResponse`.

### Supported kumite events

| Event | Required payload | Behavior |
|------|------------------|----------|
| `SCORE_DELTA` | `side`, `points`; optional `exchangeId` metadata | Compatibility single-side score path. Applies `-3/-2/-1/1/2/3`, clamps at `0`, auto-awards `SENSHU` only when this is the first positive single-side score |
| `SCORE_EXCHANGE` | `payloadJson` with `akaPoints` and/or `aoPoints`; optional `exchangeId` | Atomic scoring exchange. Applies both side deltas together; if both sides score positive points in the exchange, no `SENSHU` is awarded |
| `PENALTY` | `side` plus `penaltyLevel` and controlled `penaltyReasonCode`, or legacy `penaltyCode` | Updates canonical WKF 2026 penalty ladder or direct `KIKEN` / `SHIKKAKU` outcome |
| `TIMER_START` | - | Starts timer and moves `SCHEDULED/READY/PAUSED -> RUNNING` |
| `TIMER_STOP` | - | Applies elapsed time and moves `RUNNING -> PAUSED` |
| `TIMER_SET` | `timerMs` | Sets remaining time and stops the timer |
| `STATUS_CHANGE` | `status` | Only allows the bounded live-state transition matrix |
| `HANTEI_DECISION` | `side` | Converts `HANTEI` into a frozen, confirmable backend decision |
| `VIDEO_REVIEW_REQUEST` | `side` | Opens one active VR request, stops the timer, moves the match to `REVIEW` |
| `VIDEO_REVIEW_RESOLVE` | `resolution` plus optional correction and reason payload | Resolves VR atomically, stores an audit payload, then re-evaluates score / `SENSHU` / terminal outcome |
| `MEDICAL_START` | `side` | Starts the medical countdown, stops the timer, moves the match to `REVIEW` |
| `MEDICAL_RESOLVE` | `medicalOutcome` | Resolves medical flow; `UNFIT_TEN_SECOND_RULE` freezes a confirmable result |
| `SENSHU_REVOKE` | optional `side`, optional `penaltyReasonCode` | Revokes current `SENSHU` holder |
| `KATA_VOTE` | `judgeNumber`, `voteSide` | Still supported for kata and moves the match to `VOTING` |

### Explicitly rejected in WKF kumite flow

| Event | Behavior |
|------|----------|
| `SENSHU` | Rejected - `SENSHU` is backend-computed, not operator-toggled |
| `RESULT_CONFIRMED` | Rejected - must use `POST /api/matches/{id}/result` |
| `VR`, `KATA_REVEAL`, `MANUAL_CORRECTION` | Rejected in the current workflow |

## Flow 2: WKF Kumite Rules

### Decision profile

`KumiteRuleEngine` resolves a `KumiteRulesProfile` from `Category.rulesetVersion` and currently ships a concrete `WKF_2026` profile.

For `WKF_2026` / `WKF` matches, the time-up order is:
1. Higher total score -> `TIME_UP`
2. Equal score with `SENSHU` holder -> `SENSHU`
3. Equal score with no `SENSHU` holder and higher IPPON count -> `TIME_UP` with reason `TIME_UP_IPPON`
4. Equal score, no `SENSHU`, equal IPPON, and higher WAZA-ARI count -> `TIME_UP` with reason `TIME_UP_WAZA_ARI`
5. Equal score, no `SENSHU`, equal IPPON, and equal WAZA-ARI -> context-specific:
   - individual elimination -> `HANTEI`
   - team extra bout / tie-break bout -> `HANTEI`
   - round-robin / pool bout -> `HIKIWAKE`
   - team regular bout -> `HIKIWAKE`

`KumiteRuleEngine` receives both a `KumiteRulesProfile` and `KumiteMatchContext`. `MatchServiceImpl` derives context from existing match metadata:
- `BracketType.ROUND_ROBIN` or `POOL` -> round-robin context
- `CategoryDiscipline.TEAM_KUMITE` -> team regular bout, unless `Match.teamExtraBout=true` or `roundName` / category name contains `EXTRA`, `TIE-BREAK`, or `TIEBREAK`
- all other kumite matches -> individual elimination context

Other terminal outcomes:
- 8-point gap -> `EIGHT_POINT_LEAD`
- opponent `HANSOKU` -> `HANSOKU`
- opponent `KIKEN` -> `KIKEN`
- opponent `SHIKKAKU` -> `SHIKKAKU`
- medical timeout -> `TEN_SECOND_RULE`

### `SENSHU`

- WKF 2026 Article 12.2.2 behavior is modeled with an atomic scoring exchange:
  - a single-side first positive score awards `SENSHU`
  - if AKA and AO both score positive points in the same exchange before the signal, neither side receives `SENSHU`
  - after a simultaneous first exchange with no holder, either side can still earn `SENSHU` on a later single-side positive score
- Legacy `SCORE_DELTA` remains supported for one-sided score entry. New control surfaces should use `SCORE_EXCHANGE` when the referee awards points to both sides for the same exchange.
- Revocable by explicit `SENSHU_REVOKE` or `VIDEO_REVIEW_RESOLVE(TORIMASEN/REVOKE_SENSHU)`
- If revoked in the final 15 seconds, backend sets a re-award block for the rest of the bout

### Penalties

Canonical penalty state is per side, with one WKF 2026 ladder:
- `NONE -> CHUI_1 -> CHUI_2 -> CHUI_3 -> HANSOKU_CHUI -> HANSOKU`

`SHIKKAKU` and `KIKEN` remain direct terminal flags outside the ladder.

Controlled `penaltyReasonCode` values are `JOGAI`, `MUBOBI`, `PASSIVITY`, `AVOIDING_COMBAT`, `EXCESSIVE_CONTACT`, `GRABBING`, `WAKARETE_VIOLATION`, and `REFEREE_ORDER_VIOLATION`.

Key WKF cases enforced by backend:
- `PASSIVITY` is rejected in the first 15 seconds and final 15 seconds.
- `AVOIDING_COMBAT` in the final 15 seconds is promoted to at least `HANSOKU_CHUI`.
- If the final-15 `AVOIDING_COMBAT` offender holds `SENSHU`, backend revokes `SENSHU` and blocks re-award for the rest of the bout.

Legacy fields such as `akaChui`, `akaHansokuChui`, `akaHansoku`, `category1Level`, and `category2Level` are still populated for response compatibility, but the canonical source is `penalties.{side}.penaltyLevel`.

### Video review

- Only one active request can exist at a time
- Each side starts with one card available
- `DENIED` consumes only the requester's card. Use `reasonCode=INVALID_REQUEST`, `NO_SCORE`, or `AFTER_WAKARETE` for audit detail when the request is not valid, including technique after `WAKARETE`.
- `AWARD_SCORE` preserves the card and applies the correction before recomputing the bout. This is the valid-score path.
- `MIENAI` preserves the card and records an inconclusive review when the action cannot be seen clearly.
- `TECHNICAL_PROBLEM` preserves the card and records that the review could not be completed for technical reasons.
- `TORIMASEN` preserves the card in this workflow because it is treated as a valid correction path that removes a score and revokes `SENSHU` if that score-holder held it.
- `REVOKE_SENSHU` preserves the card and revokes only `SENSHU`. This is the WKF 2026 exception where VR can correct a corner/judges outcome related to `SENSHU`.
- VR resolve events persist structured `payloadJson` metadata: request side, resolution, resolution side / points, card kept or consumed, and optional `reasonCode` / `reasonText`.
- WKF 2026 corner-judge guard: use video review to award a valid own-athlete score or correct `SENSHU`; do not use it as a general overrule of corner judges. Requests for techniques after `WAKARETE` should be resolved as invalid and card-consuming with `reasonCode=AFTER_WAKARETE`.

### Medical workflow

- `MEDICAL_START(side)` freezes live actions and stores a backend deadline
- `MEDICAL_RESOLVE(FIT_TO_CONTINUE)` closes the workflow and returns to paused live flow
- `MEDICAL_RESOLVE(UNFIT_TEN_SECOND_RULE)` creates a frozen backend decision with `WinType.TEN_SECOND_RULE`, and marks the injured athlete's remaining `KUMITE` / `TEAM_KUMITE` entries in the same tournament as `EntryStatus.WITHDRAWN`
- If the deadline expires before a resolve event, backend auto-applies the same `TEN_SECOND_RULE` outcome on the next mutation / confirmation path
- Withdrawal propagation is athlete-scoped, not delegation-scoped: `TournamentParticipant.status` is unchanged, and `KATA` / `TEAM_KATA` entries remain eligible
- Future scheduled/ready Kumite matches containing a withdrawn entry are resolved without physical deletes: the continuing opponent is locked as winner by `TEN_SECOND_RULE`; if both sides are withdrawn or no continuing opponent exists, the match is cancelled

## Flow 3: Freeze Then Confirm

When backend determines the bout is terminal:
- timer is stopped
- `match.kumite.decision` is populated
- status becomes `RESULT_PENDING_CONFIRMATION`, except:
  - unresolved individual elimination / team extra-bout time-up becomes `HANTEI`
  - unresolved round-robin / team regular-bout time-up becomes `HIKIWAKE`

Frozen states:
- `HANTEI` = backend needs a referee winner via `HANTEI_DECISION`
- `HIKIWAKE` = backend has a no-winner draw decision awaiting confirmation
- `RESULT_PENDING_CONFIRMATION` = backend already knows the winner and only awaits confirmation

Allowed reopen paths:
- `HANTEI -> REVIEW` via `STATUS_CHANGE`
- `HIKIWAKE -> REVIEW` via `STATUS_CHANGE`
- `RESULT_PENDING_CONFIRMATION -> REVIEW` via `STATUS_CHANGE`
- `HANTEI -> RESULT_PENDING_CONFIRMATION` via `HANTEI_DECISION`

While frozen, score/timer/penalty/VR/medical live mutations are rejected.

## Flow 4: Confirm Result

`POST /api/matches/{id}/result` remains the only path to `LOCKED`.

For kumite:
- if status is `HANTEI`, confirmation is blocked until `HANTEI_DECISION` records the winner
- if status is `HIKIWAKE`, confirmation accepts `winType=HIKIWAKE` with no `winnerSide`; no winner advancement, loser advancement, tournament points, or medal placement is applied
- if status is `RESULT_PENDING_CONFIRMATION`, request `winnerSide` must match the backend decision unless the bout was reopened to `REVIEW`
- if request `winType` is null or `MANUAL`, backend reuses the frozen decision win type when available

On successful confirmation:
1. winner fields and `winType` are stored on `Match`
2. match becomes `LOCKED`
3. result and audit events are persisted
4. bracket advancement and medal logic run
5. if the tatami still points at this match, `tatami.currentMatch` is cleared
6. the locked match is published, then any next tatami match made live-ready is published so display/control pages roll forward cleanly

### TEAM_KUMITE Article 12.5 aggregate

Grouped TEAM_KUMITE uses `Match.teamMatchGroupId` as the team-match aggregate. A regular bout is still a normal `Match` with two `MatchParticipant` rows, but bracket advancement is deferred until the group resolves.

After every grouped team bout is locked, `TeamKumiteResolutionService` computes:
- `akaBoutVictories` / `aoBoutVictories` from locked regular bouts with a winner
- `akaTotalPoints` / `aoTotalPoints` from regular-bout `KumiteMatchState` scores
- whether a `teamExtraBout` exists or must be created

Resolution order follows WKF 2026 Article 12.5:
1. Higher regular bout victories wins.
2. If bout victories are tied, higher regular-bout total points wins.
3. If both are tied, backend requires an extra bout and creates a READY `TEAM_KUMITE` match with `teamExtraBout=true`.
4. The extra bout is resolved by normal Kumite rules; if points and `SENSHU` do not decide it, it may enter `HANTEI` and requires `HANTEI_DECISION`.

For grouped TEAM_KUMITE, winner advancement, loser advancement, tournament points, and medal placement run once when the aggregate winner is known. Ungrouped TEAM_KUMITE and individual KUMITE keep the existing single-match confirmation path.

## Live-State Guardrails

Current backend guardrails:
- `/events` rejects mutations on `LOCKED`, `CANCELLED`, and `COMPLETED`
- `HANTEI` accepts only `HANTEI_DECISION` or reopen-to-`REVIEW`
- `HIKIWAKE` accepts only reopen-to-`REVIEW`; confirmation uses `POST /result`
- `RESULT_PENDING_CONFIRMATION` accepts only reopen-to-`REVIEW`
- active video review blocks every other live mutation until resolved
- active medical workflow blocks every other live mutation until resolved
- `STATUS_CHANGE` cannot force `LOCKED`, `CANCELLED`, or `COMPLETED`

## Remaining Gaps

Still deferred after this upgrade:
- only `WKF_2026` / `WKF` matches use the new canonical profile in this pass
- kumite `HANTEI` is still a control-desk-entered winner, not a distributed judge-vote workflow
- historical `penaltyCategory` / C1-C2 request fields are still accepted during the migration window but are mapped into the single WKF 2026 ladder
- draw generation still creates single entry-vs-entry TEAM_KUMITE matches; it does not yet pre-create multiple grouped regular bouts or assign `teamMatchGroupId`

## Key Edge Cases

| Scenario | Behavior |
|----------|----------|
| Event on non-kumite match for kumite-only handler | `400 Bad Request` |
| Legacy `penaltyCode` omitted and explicit penalty fields omitted | `400 Bad Request` |
| Frozen match receives score/timer/penalty/VR/medical mutation | `409 Conflict` |
| VR requested with no card left | `409 Conflict` |
| Medical or VR resolution sent with no active workflow | `409 Conflict` |
| Score undo would go negative | clamped to `0` |
| First exchange has positive scores for both AKA and AO | both scores are applied; no `SENSHU` holder is set |
| Later single-side score after simultaneous first exchange | scorer receives `SENSHU` if no holder exists and re-award is not blocked |
| Round-robin / team regular bout still tied after score, `SENSHU`, IPPON, and WAZA-ARI | frozen `HIKIWAKE`, confirmable with no winner |
| Individual elimination / team extra bout still tied after score, `SENSHU`, IPPON, and WAZA-ARI | frozen `HANTEI`, requires `HANTEI_DECISION` |
| Grouped TEAM_KUMITE has tied regular bout wins and tied total points | backend creates/requires `teamExtraBout=true` match before aggregate can advance |
