---
name: club-api
description: Complete endpoint reference for club management — organizations, members, roster, attendance, fees, finance
type: api
version: "1.0"
last_updated: "2026-06-30"
metadata:
  owner: backend
  base_path: /api
source_references:
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/web/OrganizationController.java }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/web/ClubMemberController.java }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/web/ClubRosterController.java }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/web/AttendanceController.java }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/web/ClubFeeController.java }
knowledge_graph_refs:
  - { community: "club-operations", hub_node: "Organization" }
related_context_files:
  - context/features/club-roster-membership.md
  - context/features/training-attendance.md
  - context/features/club-finance-fees.md
  - context/services/business-fee-service.md
---

# API Reference: Club Management

## 2026-06-30 Updates

These notes supersede any older conflicting text below.

- Global list endpoints are now admin-only:
  - `GET /api/organizations`
  - `GET /api/persons`
  - `GET /api/athletes`
- New club-management directory endpoint:
  - `GET /api/organizations/managed-clubs`
- `GET /api/me/attendance` now returns both `sessionRows` and `leaveRequests` for member self-service.
- New org-scoped identity/athlete endpoints:
  - `GET /api/organizations/{orgId}/athletes`
  - `POST /api/organizations/{orgId}/athletes`
  - `PATCH /api/organizations/{orgId}/athletes/{athleteId}`
  - `PATCH /api/organizations/{orgId}/persons/{personId}`
- Club member write DTOs no longer accept finance summary fields (`tuitionStatus`, `tuitionPaidAmount`, `otherFeeStatus`, `otherFeePaidAmount`).
- Leave-request manager APIs are now canonicalized to:
  - `GET /api/organizations/{orgId}/attendance-leave-requests`
  - `PATCH /api/attendance-leave-requests/{requestId}/decision`
- `/api/me/attendance/leave-requests` now accepts the discriminated leave-request body with `requestType`.

All responses wrapped in `ApiResponse<T>`. Auth: `Authorization: Bearer <jwt>` required on all endpoints unless noted.

## Organizations (`/api/organizations`)

| Method | Path | Body | Response | Status |
|--------|------|------|----------|--------|
| GET | `/api/organizations` | — | `List<OrganizationResponse>` | 200 |
| GET | `/api/organizations/{id}` | — | `OrganizationResponse` | 200 |
| POST | `/api/organizations` | `OrganizationCreateRequest` | `OrganizationResponse` | 201 |
| PATCH | `/api/organizations/{id}` | `OrganizationUpdateRequest` | `OrganizationResponse` | 200 |
| DELETE | `/api/organizations/{id}` | — | — | 204 |

**OrganizationCreateRequest**:
```
@NotBlank name, shortName, code, @NotNull OrganizationType type,
country, province, address, contactEmail, contactPhone
```

**OrganizationUpdateRequest**: same fields as create, all optional + `OrganizationStatus status`.

**OrganizationType**: `CLUB, SCHOOL, ORGANIZER, FEDERATION, OTHER`
**OrganizationStatus**: `ACTIVE, INACTIVE, SUSPENDED`

Public endpoint: `GET /api/public/clubs/lookup?code=XXX` → `PublicClubLookupResponse { id, name, shortName, code }` (no auth required)

---

## Club Members (`/api/organizations/{orgId}/members`)

| Method | Path | Body | Response | Status |
|--------|------|------|----------|--------|
| GET | `/api/organizations/{orgId}/members` | — | `List<ClubMemberResponse>` | 200 |
| POST | `/api/organizations/{orgId}/members` | `ClubMemberCreateRequest` | `ClubMemberResponse` | 201 |
| PATCH | `/api/organizations/{orgId}/members/{memberId}` | `ClubMemberUpdateRequest` | `ClubMemberResponse` | 200 |
| DELETE | `/api/organizations/{orgId}/members/{memberId}` | — | — | 204 |

**ClubMemberCreateRequest** / **ClubMemberUpdateRequest** (same fields):
```
UUID personId, UUID userId,
ClubMemberRole role, ClubMemberStatus status,
LocalDate joinedAt, Boolean student, Boolean attendanceViewEnabled,
PaymentStatus tuitionStatus, BigDecimal tuitionPaidAmount,
PaymentStatus otherFeeStatus, BigDecimal otherFeePaidAmount,
String paymentNote, String memberNote
```

**ClubMemberRole**: `OWNER, MANAGER, COACH, ATHLETE, PARENT, STAFF`
**ClubMemberStatus**: `PENDING, ACTIVE, INACTIVE, LEFT, SUSPENDED`

Direct provisioning (creates Person + AppUser + OrganizationMember atomically):
```
POST /api/organizations/{orgId}/member-accounts
Body: { @NotBlank displayName, @Email email, phone, gender, birthDate, address,
        emergencyContactName, emergencyContactPhone,
        role, status, student, attendanceViewEnabled, ... }
Response: MemberAccountCreateResponse { member, username, temporaryPassword }
```

---

## Club Roster (`/api/organizations/{orgId}/roster`)

| Method | Path | Body | Response | Status |
|--------|------|------|----------|--------|
| GET | `/api/organizations/{orgId}/roster` | — | `List<ClubRosterResponse>` | 200 |
| POST | `/api/organizations/{orgId}/roster` | `ClubRosterCreateRequest` | `ClubRosterResponse` | 201 |
| PATCH | `/api/organizations/{orgId}/roster/{rosterId}` | `ClubRosterUpdateRequest` | `ClubRosterResponse` | 200 |
| DELETE | `/api/organizations/{orgId}/roster/{rosterId}` | — | — | 204 |

**ClubRosterCreateRequest**: `{ @NotNull UUID athleteId, ClubRosterStatus status, LocalDate joinedAt }`
**ClubRosterUpdateRequest**: `{ ClubRosterStatus status, LocalDate joinedAt }`
**ClubRosterStatus**: `ACTIVE, INACTIVE, INJURED, SUSPENDED`

---

## Account Requests (`/api/organizations/{orgId}/account-requests`)

| Method | Path | Body | Response | Status |
|--------|------|------|----------|--------|
| GET | `/api/organizations/{orgId}/account-requests` | — | `List<AccountRequestResponse>` | 200 |
| PATCH | `/api/organizations/{orgId}/account-requests/{requestId}/decision` | `{ status, decisionNote }` | `AccountRequestResponse` | 200 |

Public submission: `POST /api/account-requests` — no auth required.

---

## Attendance (`/api`)

| Method | Path | Body | Response | Status |
|--------|------|------|----------|--------|
| GET | `/api/organizations/{orgId}/attendance-sessions` | — | `List<AttendanceSessionResponse>` | 200 |
| POST | `/api/organizations/{orgId}/attendance-sessions` | `AttendanceSessionCreateRequest` | `AttendanceSessionResponse` | 201 |
| GET | `/api/attendance-sessions/{sessionId}` | — | `AttendanceSessionResponse` | 200 |
| PATCH | `/api/attendance-sessions/{sessionId}` | `AttendanceSessionUpdateRequest` | `AttendanceSessionResponse` | 200 |
| DELETE | `/api/attendance-sessions/{sessionId}` | — | — | 204 |
| POST | `/api/attendance-sessions/{sessionId}/records` | `AttendanceRecordRequest` | `AttendanceRecordResponse` | 201 |
| PATCH | `/api/attendance-sessions/{sessionId}/records/{recordId}` | `AttendanceRecordUpdateRequest` | `AttendanceRecordResponse` | 200 |

**AttendanceSessionCreateRequest**:
```
@NotBlank name, AttendanceSessionType type, AttendanceSessionStatus status,
Instant scheduledAt, UUID tournamentParticipantId, String notes
```

**AttendanceRecordRequest**:
```
UUID organizationMemberId, UUID athleteId (one required),
@NotNull AttendanceRecordStatus status, Instant checkInAt, String note
```

**AttendanceSessionType**: `TRAINING, FITNESS_TEST, WEIGH_IN, TOURNAMENT_CHECKIN`
**AttendanceRecordStatus**: `PRESENT, ABSENT, LATE, EXCUSED`

Leave requests:
```
POST /api/me/attendance/leave-requests
Body: { @NotNull UUID sessionId, @NotBlank String reason }
Response: LeaveRequestResponse (201)

PATCH /api/attendance-leave-requests/{requestId}/decision
Body: { status: APPROVED|REJECTED, decisionNote? }
```

Training schedule:
```
POST /api/organizations/{orgId}/training-schedule/ensure-today   — trigger today's session generation
POST /api/organizations/{orgId}/training-schedule/day-off        — mark day as no training
```

---

## Fees & Finance (`/api/organizations/{orgId}`)

### Fee Roles
| Method | Path | Body | Response | Status |
|--------|------|------|----------|--------|
| POST | `/fee-roles` | `ClubFeeRoleRequest` | `ClubFeeRoleResponse` | 201 |
| PATCH | `/fee-roles/{roleId}` | `ClubFeeRoleRequest` | `ClubFeeRoleResponse` | 200 |
| DELETE | `/fee-roles/{roleId}` | — | — | 204 |
| PUT | `/members/{memberId}/fee-roles` | `{ List<UUID> feeRoleIds }` | `MemberFeeRoleResponse` | 200 |
| PUT | `/members/fee-roles/bulk` | `BulkMemberFeeRoleUpdateRequest` | `List<MemberFeeRoleResponse>` | 200 |

**BulkMemberFeeRoleUpdateRequest**: `{ List<UUID> memberIds, List<UUID> feeRoleIds, Mode mode }`
**Mode**: `ADD, REPLACE, REMOVE` (default REPLACE)

### Fee Items
| Method | Path | Body | Response | Status |
|--------|------|------|----------|--------|
| POST | `/fee-items` | `ClubFeeItemRequest` | `ClubFeeItemResponse` | 201 |
| PATCH | `/fee-items/{feeItemId}` | `ClubFeeItemRequest` | `ClubFeeItemResponse` | 200 |
| DELETE | `/fee-items/{feeItemId}` | — | — | 204 |
| POST | `/fee-items/{feeItemId}/apply` | `ApplyFeeItemRequest` | `List<MemberFeeAssignmentResponse>` | 200 |

**ClubFeeItemRequest**:
```
@NotBlank name, FeeItemType feeType, FeeItemKind feeKind, BillingCycle billingCycle,
FeeItemStatus status, BigDecimal defaultAmount, Integer dueDay, String description,
List<FeeRoleAmountRequest> roleAmounts
```

**ApplyFeeItemRequest**:
```
List<UUID> memberIds, List<UUID> feeRoleIds, Boolean applyToAllActive,
LocalDate dueDate, String note
```

### Assignments & Finance
| Method | Path | Body | Response | Status |
|--------|------|------|----------|--------|
| PATCH | `/fee-assignments/{assignmentId}` | `MemberFeeAssignmentUpdateRequest` | `MemberFeeAssignmentResponse` | 200 |
| DELETE | `/fee-assignments/{assignmentId}` | — | — | 204 |
| GET | `/fees/overview` | — | `ClubFeeOverviewResponse` | 200 |
| GET | `/finance/overview` | — | `ClubFeeOverviewResponse` | 200 |
| PUT | `/finance/tuition-overrides/bulk` | `{ List<UUID> memberIds, UUID feeItemId }` | `List<MemberTuitionOverrideResponse>` | 200 |
| GET | `/finance/expenses` | — | `List<ClubFinanceExpenseResponse>` | 200 |
| POST | `/finance/expenses` | `ClubFinanceExpenseRequest` | `ClubFinanceExpenseResponse` | 201 |
| PATCH | `/finance/expenses/{expenseId}` | `ClubFinanceExpenseRequest` | `ClubFinanceExpenseResponse` | 200 |
| DELETE | `/finance/expenses/{expenseId}` | — | — | 204 |

**MemberFeeAssignmentUpdateRequest**:
```
BigDecimal amountDue, BigDecimal paidAmount, PaymentStatus status,
LocalDate dueDate, String note
```

---

## Known Inconsistencies

- Two overlapping paths: `GET /fees/overview` and `GET /finance/overview` return same type (`ClubFeeOverviewResponse`) — likely duplicates for backward compat
- No pagination on any list endpoint
- `DELETE` on sessions only works for MANUAL source sessions (SCHEDULED sessions may be guarded)
