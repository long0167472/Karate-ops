---
name: fee-entities
description: Entity schema for the fee system — ClubFeeRole, ClubFeeItem, ClubFeeItemRoleAmount, MemberFeeAssignment, TuitionOverride
type: data-model
version: "1.0"
last_updated: "2026-06-11"
metadata:
  owner: backend
source_references:
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/entity/ClubFeeRole.java }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/entity/ClubFeeItem.java }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/entity/MemberFeeAssignment.java }
knowledge_graph_refs:
  - { community: "club-operations", hub_node: "Organization" }
related_context_files:
  - context/services/business-fee-service.md
  - context/features/club-finance-fees.md
  - context/data-models/person-club-entities.md
---

# Data Model: Fee Entities

## Entity Map

```
Organization
  ├── ClubFeeRole (pricing tier)
  │       └── ClubFeeItemRoleAmount (per-role price for an item)
  │
  ├── ClubFeeItem (fee product: tuition, uniform, etc.)
  │       └── ClubFeeItemRoleAmount
  │
  ├── OrganizationMember
  │       ├── OrganizationMemberFeeRole (member → fee role assignments)
  │       ├── OrganizationMemberTuitionOverride (override default tuition)
  │       └── MemberFeeAssignment (individual charge record)
  │
  └── ClubFinanceExpense (expenditure tracking)
```

---

## ClubFeeRole

**Table**: `club_fee_roles`

| Field | Type | Nullable | Default | Notes |
|-------|------|----------|---------|-------|
| `organization` | Organization | No | — | FK EAGER |
| `code` | String(80) | No | — | Unique per org; trimmed uppercase |
| `name` | String(160) | No | — | Display name |
| `description` | String(500) | Yes | — | |
| `priority` | int | No | 100 | Display ordering |
| `active` | boolean | No | true | |

Default roles auto-created by V7/V8 migration per org:

| code | name | typical discount |
|------|------|-----------------|
| `NORMAL` | Học viên thường | none (full price) |
| `STUDENT` | Sinh viên | 20% off |
| `ATHLETE` | VĐV đội tuyển | 40% off |
| `CLUB_STAFF` | Ban cán sự CLB | 60% off |

**[NEEDS HUMAN INPUT]**: Are discount amounts stored as absolute values or percentages in `ClubFeeItemRoleAmount`?

---

## ClubFeeItem

**Table**: `club_fee_items`

| Field | Type | Nullable | Default | Notes |
|-------|------|----------|---------|-------|
| `organization` | Organization | No | — | FK EAGER |
| `name` | String(180) | No | — | |
| `feeType` | FeeItemType | No | `TUITION` | Enum |
| `feeKind` | FeeItemKind | No | `ONE_TIME_INCOME` | Enum (inferred by service) |
| `billingCycle` | BillingCycle | No | `MONTHLY` | Enum |
| `status` | FeeItemStatus | No | `ACTIVE` | Enum |
| `defaultAmount` | BigDecimal | No | 0 | Fallback if no role match |
| `dueDay` | Integer | Yes | — | Day of month for due date (clamped 1–28) |
| `description` | String(500) | Yes | — | |

**FeeItemType**: `TUITION, UNIFORM, EXAM, TOURNAMENT, OTHER`
**FeeItemKind**: `MONTHLY_TUITION_DEFAULT, MONTHLY_TUITION_OVERRIDE, ONE_TIME_INCOME`
**BillingCycle**: `ONE_TIME, MONTHLY, QUARTERLY, YEARLY`
**FeeItemStatus**: `DRAFT, ACTIVE, ARCHIVED`

Each org has exactly one `MONTHLY_TUITION_DEFAULT` (created by `ensureDefaultTuitionItem()`):
- name = `"Học phí"`, dueDay = 10, defaultAmount = 0

---

## ClubFeeItemRoleAmount

**Table**: `club_fee_item_role_amounts`

| Field | Type | Nullable | Default | Notes |
|-------|------|----------|---------|-------|
| `feeItem` | ClubFeeItem | No | — | FK EAGER |
| `feeRole` | ClubFeeRole | No | — | FK EAGER |
| `amount` | BigDecimal | No | 0 | Price for this role |
| `exempt` | boolean | No | false | true → member pays 0, status=WAIVED |

`resolveAmount()` selects the MINIMUM amount among all matching role amounts (unless exempt).

---

## OrganizationMemberFeeRole

**Table**: `organization_member_fee_roles`

| Field | Type | Nullable | Notes |
|-------|------|----------|-------|
| `organization` | Organization | No | FK EAGER |
| `member` | OrganizationMember | No | FK EAGER |
| `feeRole` | ClubFeeRole | No | FK EAGER |

Links a member to one or more fee roles. Can be set/replaced via `PUT /members/{memberId}/fee-roles`.

---

## MemberFeeAssignment

**Table**: `member_fee_assignments`

| Field | Type | Nullable | Default | Notes |
|-------|------|----------|---------|-------|
| `organization` | Organization | No | — | FK EAGER |
| `member` | OrganizationMember | No | — | FK EAGER |
| `feeItem` | ClubFeeItem | No | — | FK EAGER |
| `assignedRole` | ClubFeeRole | Yes | — | FK EAGER; which fee role was used in resolveAmount |
| `amountDue` | BigDecimal | No | 0 | Resolved amount |
| `paidAmount` | BigDecimal | No | 0 | Running payment total |
| `status` | PaymentStatus | No | `PENDING` | |
| `dueDate` | LocalDate | Yes | — | Computed from dueDay or override |
| `source` | FeeAssignmentSource | No | `RULE` | `RULE` = batch apply, `MANUAL` = direct |
| `note` | String(500) | Yes | — | |

**PaymentStatus**: `PAID, PENDING, OVERDUE, WAIVED, PARTIAL` (enum is `PaymentStatus` — note: no `CANCELLED` value)
**FeeAssignmentSource**: `RULE, MANUAL`

No idempotency key — running `applyFeeItem()` twice creates duplicate assignments. See [known-issues/patterns.md Pattern 3].

---

## OrganizationMemberTuitionOverride

**Table**: `org_member_tuition_overrides`

| Field | Type | Nullable | Notes |
|-------|------|----------|-------|
| `organization` | Organization | No | FK |
| `member` | OrganizationMember | No | FK |
| `feeItem` | ClubFeeItem | No | FK; must be `MONTHLY_TUITION_OVERRIDE` kind |

Used to give a member a personalized tuition fee item. Service reads this before falling back to `MONTHLY_TUITION_DEFAULT`.

---

## ClubFinanceExpense

**Table**: `club_finance_expenses`

| Field | Type | Nullable | Notes |
|-------|------|----------|-------|
| `organization` | Organization | No | FK |
| `name` | String | No | |
| `amount` | BigDecimal | No | |
| `expenseDate` | LocalDate | Yes | |
| `status` | ExpenseDisbursementStatus | No | `PENDING` |
| `note` | String(500) | Yes | |

**ExpenseDisbursementStatus**: `PENDING_DISBURSEMENT, DISBURSED`

Finance summary: `cashSurplus = totalPaid - expensesDisbursed`
