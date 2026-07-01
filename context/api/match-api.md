---
name: match-api
description: Match event recording, scoring commands, timer control, kata voting, and result confirmation endpoints
type: api
version: "1.3"
last_updated: "2026-07-01"
metadata:
  owner: backend
  base_path: /api/matches
source_references:
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/web/MatchController.java }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/service/impl/MatchServiceImpl.java }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/rules/KumiteRuleEngine.java }
related_context_files:
  - context/features/tatami-match-scoring.md
  - context/data-models/match-entities.md
---

# API Reference: Match Control

## Endpoints

| Method | Path | Body | Response |
|--------|------|------|----------|
| `GET` | `/api/tournaments/{tournamentId}/matches` | - | `List<MatchResponse>` |
| `GET` | `/api/matches/{id}` | - | `MatchResponse` |
| `POST` | `/api/matches/{id}/events` | `MatchEventRequest` | `MatchResponse` |
| `POST` | `/api/matches/{id}/result` | `ConfirmResultRequest` | `MatchResponse` |

## MatchEventRequest

```java
public record MatchEventRequest(
    ScoreEventType type,
    Side side,
    Integer points,
    String penaltyCode,
    Integer judgeNumber,
    Side voteSide,
    Integer timerMs,
    MatchStatus status,
    KumitePenaltyCategory penaltyCategory, // legacy compatibility only
    KumitePenaltyLevel penaltyLevel,
    String penaltyReasonCode,
    VideoReviewResolution resolution,
    Side resolutionSide,
    Integer resolutionPoints,
    String reasonCode,
    String reasonText,
    MedicalOutcome medicalOutcome,
    String exchangeId,
    String payloadJson
)
```

## `/events` support matrix

| Event | Required fields | Behavior |
|------|------------------|----------|
| `SCORE_DELTA` | `side`, `points`; optional `exchangeId` | Compatibility single-side score path. Apply `-3/-2/-1/1/2/3`, clamp at `0`, auto-award `SENSHU` only when this is the first positive single-side score |
| `SCORE_EXCHANGE` | `payloadJson` with `akaPoints` and/or `aoPoints`; optional `exchangeId` | Apply an atomic scoring exchange. If AKA and AO both receive positive points in the exchange, no `SENSHU` is awarded |
| `PENALTY` | `side` plus `penaltyLevel` and controlled `penaltyReasonCode`, or legacy `penaltyCode` | Update canonical WKF 2026 penalty ladder or direct `KIKEN` / `SHIKKAKU` |
| `TIMER_START` | - | Start kumite timer |
| `TIMER_STOP` | - | Stop timer and apply elapsed time |
| `TIMER_SET` | `timerMs` | Set remaining time and stop timer |
| `STATUS_CHANGE` | `status` | Allowed only inside the live-state transition matrix |
| `HANTEI_DECISION` | `side` | Record referee winner when status is `HANTEI` |
| `VIDEO_REVIEW_REQUEST` | `side` | Open VR request and move the match to `REVIEW` |
| `VIDEO_REVIEW_RESOLVE` | `resolution` plus optional correction and reason payload | Resolve VR atomically and persist structured audit metadata |
| `MEDICAL_START` | `side` | Start backend medical countdown and move the match to `REVIEW` |
| `MEDICAL_RESOLVE` | `medicalOutcome` | Resolve medical workflow |
| `SENSHU_REVOKE` | optional `side`, optional `penaltyReasonCode` | Revoke current `SENSHU` |
| `KATA_VOTE` | `judgeNumber`, `voteSide` | Save / update kata vote |
| `SENSHU` | - | Rejected - backend computes `SENSHU` |
| `RESULT_CONFIRMED` | - | Rejected - use `/result` |
| `VR`, `KATA_REVEAL`, `MANUAL_CORRECTION` | - | Rejected in current workflow |

## Explicit kumite payloads

### Score exchange

Use this when both competitors score in the same referee exchange. `SCORE_DELTA` remains compatible for one-sided score buttons.

```json
{
  "type": "SCORE_EXCHANGE",
  "exchangeId": "exchange-1719300000000",
  "payloadJson": "{\"exchangeId\":\"exchange-1719300000000\",\"akaPoints\":1,\"aoPoints\":1}"
}
```

Accepted `payloadJson` shapes:
- top-level `akaPoints` / `aoPoints`
- nested `deltas` object with `AKA` / `AO` or `aka` / `ao`

SENSHU rule:
- exactly one positive scorer in the exchange can receive first unopposed-score `SENSHU`
- positive scores for both AKA and AO in the same exchange apply to the score but award no `SENSHU`

### Penalty event

Preferred contract for new FE work:

```json
{
  "type": "PENALTY",
  "side": "AKA",
  "penaltyLevel": "CHUI_2",
  "penaltyReasonCode": "JOGAI"
}
```

`penaltyLevel` values:
- `NONE`
- `CHUI_1`
- `CHUI_2`
- `CHUI_3`
- `HANSOKU_CHUI`
- `HANSOKU`

Controlled `penaltyReasonCode` values:
- `JOGAI`
- `MUBOBI`
- `PASSIVITY`
- `AVOIDING_COMBAT`
- `EXCESSIVE_CONTACT`
- `GRABBING`
- `WAKARETE_VIOLATION`
- `REFEREE_ORDER_VIOLATION`

WKF 2026 guards:
- `PASSIVITY` is rejected in the first 15 seconds and final 15 seconds.
- `AVOIDING_COMBAT` in the final 15 seconds is promoted to at least `HANSOKU_CHUI`.
- Final-15 `AVOIDING_COMBAT` revokes `SENSHU` when the offender is the holder.

Compatibility window:
- legacy `penaltyCode` is still accepted and mapped server-side
- legacy `penaltyCategory` is still accepted but ignored as a canonical category; the ladder is single per side
- new FE code should stop sending `penaltyCode`

### Hantei decision

```json
{ "type": "HANTEI_DECISION", "side": "AO" }
```

### Video review request / resolve

```json
{ "type": "VIDEO_REVIEW_REQUEST", "side": "AKA" }
```

```json
{
  "type": "VIDEO_REVIEW_RESOLVE",
  "resolution": "AWARD_SCORE",
  "resolutionSide": "AKA",
  "resolutionPoints": 2
}
```

Valid `resolution` values:
- `DENIED`
- `AWARD_SCORE`
- `TORIMASEN`
- `REVOKE_SENSHU`
- `MIENAI`
- `TECHNICAL_PROBLEM`

Card behavior:
- `DENIED` consumes only the requester's card. Use `reasonCode=INVALID_REQUEST`, `NO_SCORE`, or `AFTER_WAKARETE` to distinguish invalid requests and no-score outcomes.
- `AWARD_SCORE`, `MIENAI`, `TECHNICAL_PROBLEM`, `TORIMASEN`, and `REVOKE_SENSHU` preserve the requester's card.
- `TORIMASEN` is treated as a valid correction path in KarateOps and removes the specified score, also revoking `SENSHU` when that score-holder held it.
- `REVOKE_SENSHU` revokes only `SENSHU` and is the explicit WKF 2026 corner-judge overrule exception.
- Techniques after `WAKARETE` should not become valid VR score requests. Resolve them as `DENIED` with `reasonCode=AFTER_WAKARETE` so the card is consumed and the audit log is clear.

VR request / resolve events expose `payloadJson` in `recentEvents`; for VR it is backend-structured audit JSON containing `requestingSide`, `resolution`, `resolutionSide`, `resolutionPoints`, `cardConsumed`, `cardKept`, `reasonCode`, `reasonText`, and optional operator payload.

### Medical start / resolve

```json
{ "type": "MEDICAL_START", "side": "AO" }
```

```json
{ "type": "MEDICAL_RESOLVE", "medicalOutcome": "UNFIT_TEN_SECOND_RULE" }
```

Valid `medicalOutcome` values:
- `FIT_TO_CONTINUE`
- `UNFIT_TEN_SECOND_RULE`
- `CANCELLED`

## MatchResponse kumite contract

`MatchResponse.kumite` now includes explicit backend-owned sub-objects:
- `decision`: `winnerSide`, `winType`, `reasonCode`, `reasonText`, `frozen`, `confirmable`
- `senshu`: `holderSide`, `awardedAt`, `revoked`, `revokedAt`, `revocationReasonCode`
- `penalties`: per-side canonical `penaltyLevel`, `reasonCode`, compatibility `category1Level`, `category2Level`, plus `hansoku`, `shikkaku`, `kiken`
- `videoReview`: `activeRequestSide`, `status`, `akaCardAvailable`, `aoCardAvailable`, `lastResolution`
- `medical`: `injuredSide`, `startedAt`, `deadlineAt`, `status`, `lastOutcome`

Legacy compatibility fields remain on the same payload for one migration window:
- `akaSenshu`, `aoSenshu`
- `akaChui`, `aoChui`
- `akaHansokuChui`, `aoHansokuChui`
- `akaHansoku`, `aoHansoku`
- `akaShikkaku`, `aoShikkaku`
- `akaKiken`, `aoKiken`

## Kumite resolution semantics

For `WKF_2026` / `WKF` matches:
1. Higher total score at time-up -> `TIME_UP`
2. Equal score with `SENSHU` holder -> `SENSHU`
3. Equal score with no `SENSHU`, then higher IPPON count -> `TIME_UP` with `TIME_UP_IPPON`
4. Equal score, no `SENSHU`, equal IPPON, then higher WAZA-ARI count -> `TIME_UP` with `TIME_UP_WAZA_ARI`
5. Still equal after score, `SENSHU`, IPPON, and WAZA-ARI:
   - individual elimination / team extra bout -> `HANTEI`
   - round-robin / pool / team regular bout -> `HIKIWAKE`

Other terminal paths:
- 8-point gap -> `EIGHT_POINT_LEAD`
- foul ladder reaches `HANSOKU` -> opponent wins by `HANSOKU`
- direct `KIKEN` / `SHIKKAKU` -> opponent wins accordingly
- medical timeout -> `TEN_SECOND_RULE`

## ConfirmResultRequest

```java
public record ConfirmResultRequest(
    Side winnerSide, // nullable only when confirming HIKIWAKE
    WinType winType,
    String reason
)
```

## WinType values

| WinType | Meaning |
|--------|---------|
| `POINTS` | Manual points-based confirmation when no frozen backend decision applies |
| `EIGHT_POINT_LEAD` | 8-point gap |
| `TIME_UP` | Higher score at time-up |
| `SENSHU` | Time-up tie broken by `SENSHU` |
| `HANTEI` | Referee decision after score-tied / no-senshu time-up |
| `HIKIWAKE` | No-winner draw for round-robin / team regular bout after all WKF time-up tie-breakers remain equal |
| `KIKEN` | Opponent withdrew |
| `HANSOKU` | Opponent lost by foul ladder |
| `SHIKKAKU` | Opponent expelled |
| `TEN_SECOND_RULE` | Opponent failed the medical 10-second rule |
| `DISQUALIFICATION` | Legacy value retained for historical compatibility |
| `KATA_VOTES` | Kata majority vote winner |
| `BYE` | Auto-advance |
| `MANUAL` | Fallback explicit confirmation |

## Confirm result behavior

`POST /result` is the only path to `LOCKED`.

For kumite:
- `HANTEI` cannot be confirmed until `HANTEI_DECISION` records the winner
- `HIKIWAKE` is confirmed with `winType=HIKIWAKE` and no `winnerSide`; backend locks the match with no winner fields and no bracket advancement
- `RESULT_PENDING_CONFIRMATION` can only be confirmed for the backend-decided `winnerSide`, unless the match was reopened to `REVIEW`
- if request `winType` is null or `MANUAL`, backend reuses the frozen decision win type when available

Successful confirmation also:
- persists result and audit events
- advances bracket links
- clears `tatami.currentMatch` when it still points at the locked match
- publishes the locked match and any downstream match promoted to tatami-live readiness

## Guardrails

- `/events` rejects mutations on `LOCKED`, `CANCELLED`, and `COMPLETED`
- `HANTEI` only accepts `HANTEI_DECISION` or reopen-to-`REVIEW`
- `HIKIWAKE` only accepts reopen-to-`REVIEW`; use `/result` to confirm the draw
- `RESULT_PENDING_CONFIRMATION` only accepts reopen-to-`REVIEW`
- active VR blocks every other live mutation until resolved
- active medical blocks every other live mutation until resolved
- `STATUS_CHANGE` cannot force `LOCKED`, `CANCELLED`, or `COMPLETED`

## Important error cases

| Scenario | HTTP |
|----------|------|
| Invalid `penaltyCode` or explicit penalty payload | `400` |
| Invalid `points` or `resolutionPoints` | `400` |
| Unsupported enum event sent to `/events` | `400` |
| `RESULT_CONFIRMED` sent to `/events` | `400` |
| Frozen match receives forbidden live mutation | `409` |
| VR requested with no card left | `409` |
| Hantei confirmation attempted before `HANTEI_DECISION` | `409` |
| Winner side has no participant | `409` |
| Match not found | `404` |
