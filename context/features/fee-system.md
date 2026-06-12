---
name: fee-system
description: Fee item lifecycle — creation, billing cycle, due date computation, member assignment
type: feature
last_updated: 2026-06-11
criticality: high
metadata:
  owner: backend
  affected_services: [karate-tournament-backend, karate-ops-fe]
  affected_domains: [clubs, fees]
source_references:
  - symbol: ClubFeeServiceImpl
    file: karate-tournament-backend/src/main/java/com/karate/tournament/service/impl/ClubFeeServiceImpl.java
  - symbol: ClubFeeServiceImpl.defaultDueDate
    file: karate-tournament-backend/src/main/java/com/karate/tournament/service/impl/ClubFeeServiceImpl.java
  - symbol: BillingCycle
    file: karate-tournament-backend/src/main/java/com/karate/tournament/entity/enums/BillingCycle.java
  - symbol: FeesTab
    file: karate-ops-fe/src/features/clubs/components/FeesTab.tsx
  - symbol: ApplyFeeItemRequest
    file: karate-tournament-backend/src/main/java/com/karate/tournament/dto/request/ApplyFeeItemRequest.java
---

# Fee System

## Data model

```
ClubFeeRole
  └─ ClubFeeItem  (feeKind, dueDay, billingCycle)
       └─ ClubFeeItemRoleAmount  (per-role amount)
            └─ OrganizationMemberFeeRole  (per-member role assignment)
                 └─ fee assignment record
```

## BillingCycle enum

`ONE_TIME | MONTHLY | QUARTERLY | YEARLY`

## Due date computation (BE-owned, server clock)

`ClubFeeServiceImpl.defaultDueDate()`:
1. Take `dueDay` from the fee item; default to `10` if null
2. Clamp to `[1, 28]` — avoids month-end edge cases (Feb 28, short months)
3. Return `LocalDate.now().withDayOfMonth(dueDay)`

If `ApplyFeeItemRequest.dueDate` is non-null, that explicit value wins.  
Otherwise BE computes the default. **Client clock is never used.**

## BillingCycle defaulting (BE-owned)

If `ApplyFeeItemRequest.billingCycle` is null:
- `feeKind == ONE_TIME_INCOME` → `BillingCycle.ONE_TIME`
- otherwise → `BillingCycle.MONTHLY`

## FE contract (`FeesTab` → `ApplyFeeItemRequest`)

FE sends: `memberIds`, `feeRoleIds`, `applyToAllActive`, `dueDate` (optional user override), `note`.  
FE does NOT send `billingCycle` — always null, BE infers.  
FE does NOT compute `dueDate` from client clock — omit if user didn't explicitly pick one.
