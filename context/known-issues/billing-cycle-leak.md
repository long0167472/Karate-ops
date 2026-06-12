---
name: billing-cycle-leak
description: FE was computing due dates and defaulting billingCycle — fixed 2026-06-11, rule extracted
type: known-issue
last_updated: 2026-06-11
criticality: medium
metadata:
  owner: backend
  affected_services: [karate-ops-fe, karate-tournament-backend]
  affected_domains: [fees]
source_references:
  - symbol: ClubFeeServiceImpl.defaultDueDate
    file: karate-tournament-backend/src/main/java/com/karate/tournament/service/impl/ClubFeeServiceImpl.java
  - symbol: FeesTab
    file: karate-ops-fe/src/features/clubs/components/FeesTab.tsx
---

# BillingCycle Logic Leak (resolved)

## Status

**Fixed** — 2026-06-11. Verify: `FeesTab.tsx` must not import or reference `BillingCycle`.

## What happened

`FeesTab.tsx` had a `dueDateForItem()` function that computed due dates from the **client clock**, and hardcoded `billingCycle` defaults (`ONE_TIME_INCOME → ONE_TIME`, else `MONTHLY`) — duplicating `ClubFeeServiceImpl.inferFeeKind()` in BE.

**Risk:** Any change to the BE billing rule left FE silently returning the old value. The two could drift invisibly.

## How it was found

`BillingCycle` appeared as the highest-betweenness bridge node in the graphify knowledge graph — it connected both FE and BE communities. Tracing the bridge revealed the duplication. This is the canonical use case for bridge-node analysis.

## Fix

| Side | Change |
|------|--------|
| BE | Added `ClubFeeServiceImpl.defaultDueDate()` — server clock, clamped [1,28], default dueDay=10 |
| BE | `billingCycle` defaulting inferred from `feeKind` inside `ClubFeeServiceImpl` |
| FE | Removed `dueDateForItem()`, removed `billingCycle` from all payloads, removed `BillingCycle` import |

## Rule extracted

→ [coding-standards.md Rule 1](../rules/coding-standards.md) — Business logic belongs in BE, never FE.
