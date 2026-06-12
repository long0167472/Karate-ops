---
name: business-fee-service
description: Fee assignment, resolveAmount algorithm, billing cycle defaulting, tuition override, finance summary
type: service
version: "1.0"
last_updated: "2026-06-11"
criticality: HIGH
metadata:
  owner: backend
  affected_services: [karate-tournament-backend]
  affected_domains: [clubs, fees]
source_references:
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/service/impl/ClubFeeServiceImpl.java, note: "Full fee business logic" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/entity/ClubFeeItem.java, note: "Fee item entity" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/entity/MemberFeeAssignment.java, note: "Assignment entity" }
knowledge_graph_refs:
  - { community: "club-operations", hub_node: "Organization" }
related_context_files:
  - context/features/club-finance-fees.md
  - context/data-models/fee-entities.md
  - context/known-issues/patterns.md
---

# Service: Club Fee Service

## Responsibilities
1. Fee role management (create/update/delete `ClubFeeRole`)
2. Assign fee roles to members
3. Create and manage fee items (`ClubFeeItem`)
4. Apply fee items to members → `MemberFeeAssignment` records
5. Resolve per-member fee amount via role-based pricing
6. Track and summarize club finance

## resolveAmount() Algorithm

The core pricing engine. Called for each member when a fee item is applied.

```
Input: OrganizationMember member, ClubFeeItem item

1. If item.feeKind == ONE_TIME_INCOME:
   → Return ResolvedAmount(role=null, amount=item.defaultAmount)
   (no role lookup — flat amount for all)

2. Load member's fee roles:
   memberFeeRoles = memberFeeRoleRepository.findByMember(member)
   if empty → skip role lookup, fall through to defaultAmount

3. Load ClubFeeItemRoleAmounts for item and those roles:
   roleAmounts = roleAmountRepository.findByFeeItemAndRoleIn(item, memberFeeRoles)

4. If no matching role amounts:
   → Return ResolvedAmount(role=null, amount=item.defaultAmount)

5. Filter for exempt roles:
   if any roleAmount.exempt == true:
     → Return ResolvedAmount(role=that role, amount=BigDecimal.ZERO)

6. Select MINIMUM amount among all matched role amounts:
   → Return ResolvedAmount(role=role with min amount, amount=min amount)
```

**Why minimum**: Members may have multiple fee roles (e.g., STUDENT + ATHLETE). The cheapest applicable rate wins — favorable to the member.

**Assignment status from resolved amount**:
- `amount == 0` OR `exempt == true` → `MemberFeeAssignmentStatus.WAIVED`
- `amount > 0` → `MemberFeeAssignmentStatus.PENDING`

## defaultDueDate() Computation

Called when applying a fee item to set the due date on each assignment.

```java
LocalDate defaultDueDate(ClubFeeItem item) {
    int safeDay = Math.max(1, Math.min(28, item.dueDay));
    return LocalDate.now().withDayOfMonth(safeDay);
}
```

- `dueDay` is clamped to `[1, 28]` — never overflows short months
- Default `dueDay = 10` (set in `ensureDefaultTuitionItem()`)
- If `ApplyFeeItemRequest.dueDate` is provided, it overrides this computation

## billingCycle Defaulting (inferFeeKind / applyFeeItem)

When creating/updating a `ClubFeeItem`:

```
If request.billingCycle provided → use it
Else if feeKind == ONE_TIME_INCOME → BillingCycle.ONE_TIME
Else → BillingCycle.MONTHLY
```

`inferFeeKind()`:
```
If feeType == TUITION AND billingCycle == MONTHLY → FeeItemKind.MONTHLY_TUITION_OVERRIDE
Else → FeeItemKind.ONE_TIME_INCOME
```

**Note**: FE must send `billingCycle = null` to let BE apply the default. This was the BillingCycle leak bug (fixed 2026-06-11). See [known-issues/patterns.md Pattern 1].

## Fee Item Kinds

| Kind | Description | billingCycle |
|------|-------------|-------------|
| `MONTHLY_TUITION_DEFAULT` | Auto-created default tuition per org | MONTHLY |
| `MONTHLY_TUITION_OVERRIDE` | Per-member or per-group override | MONTHLY |
| `ONE_TIME_INCOME` | One-time fee (event, uniform, etc.) | ONE_TIME |

Only one `MONTHLY_TUITION_DEFAULT` exists per organization (created by `ensureDefaultTuitionItem()`).

## Default Tuition Item Bootstrap

On every `overview()` call, `ensureDefaultTuitionItem(organization)` is called:
```
If no MONTHLY_TUITION_DEFAULT for this org:
  Create ClubFeeItem {
    name = "Học phí",
    feeType = TUITION,
    feeKind = MONTHLY_TUITION_DEFAULT,
    billingCycle = MONTHLY,
    status = ACTIVE,
    defaultAmount = 0,
    dueDay = 10,
    description = "Khoản học phí tháng mặc định của CLB."
  }
```

## Monthly Tuition Resolution for a Member

```
monthlyTuitionForMember(member, defaultItem):
  override = tuitionOverrides.findByOrganizationAndMember(org, member)
  if override exists AND override.feeItem.status == ACTIVE:
    return override.feeItem
  else:
    return defaultItem
```

This allows per-member tuition override (e.g., scholarship students pay different rate).

## applyFeeItem() — Batch Assignment

```
POST /api/organizations/{orgId}/fee-items/{feeItemId}/apply
```

**Target member selection** (`targetMembers()`):
```
start with LinkedHashMap<UUID, OrganizationMember>
if request.applyToAllActive:
  add all ACTIVE members of org
if request.memberIds not empty:
  add those specific members
if request.feeRoleIds not empty:
  add members who have any of those fee roles
Return deduplicated list (LinkedHashMap preserves insertion order)
```

**For each target member**:
1. Skip if assignment already exists for `(member, feeItem)` (no idempotency key — see [known-issues/patterns.md Pattern 3])
2. Call `resolveAmount(member, feeItem)`
3. Create `MemberFeeAssignment`:
   ```
   {
     member, feeItem,
     feeRole = resolvedAmount.role,
     amountDue = resolvedAmount.amount,
     status = WAIVED (if amount 0) or PENDING,
     dueDate = request.dueDate ?? defaultDueDate(feeItem)
   }
   ```

## Finance Summary (ClubFinanceSummaryResponse)

Computed over all assignments for an organization:

| Field | Computation |
|-------|-------------|
| `activeMembers` | count of ACTIVE OrganizationMembers |
| `monthlyExpected` | sum of active members' resolved tuition amounts |
| `oneTimeDue` | sum of ONE_TIME_INCOME assignments with status PENDING |
| `totalDue` | sum of all PENDING amountDue |
| `totalPaid` | sum of all paidAmount across assignments |
| `outstanding` | totalDue - totalPaid |
| `expensesTotal` | sum of all expense amounts |
| `expensesDisbursed` | sum of DISBURSED expense amounts |
| `expensesPending` | sum of PENDING expense amounts |
| `cashSurplus` | totalPaid - expensesDisbursed |

## Fee Role Management

`ClubFeeRole` fields:
- `code` — unique per org (trimmed, uppercase)
- `name` — display name
- `description`
- `priority` — integer; used for display ordering
- `active` — boolean

Default fee roles auto-created per org by V7/V8 migrations:

| code | name | discount |
|------|------|---------|
| `NORMAL` | Học viên thường | 100% (full price) |
| `STUDENT` | Sinh viên | 80% |
| `ATHLETE` | VĐV đội tuyển | 60% |
| `CLUB_STAFF` | Ban cán sự CLB | 40% |

**[NEEDS HUMAN INPUT]**: Are default role amounts set relative to defaultAmount (percentage), or as absolute values?

## bulkSetTuitionOverrides()

Allows setting per-member tuition overrides in batch:
```
DELETE existing overrides for each member in request
If request.feeItemId provided:
  Validate feeItem is MONTHLY_TUITION_OVERRIDE AND billingCycle == MONTHLY (not DEFAULT)
  Create OrganizationMemberTuitionOverride { organization, member, feeItem }
```

Validation fails with `BadRequestException` if fee item is `MONTHLY_TUITION_DEFAULT` (cannot override with the default).

## MemberFeeAssignment PaymentStatus Values
The assignment uses the shared `PaymentStatus` enum: `PENDING, PARTIAL, PAID, WAIVED, OVERDUE`
Note: there is no `CANCELLED` value in this enum.

## Known Issues
- Pattern 3: `applyFeeItem()` has no idempotency key. Running twice creates duplicate assignments for the same (member, feeItem) in different invocations (only filters existing by exact feeItem match in same run).
