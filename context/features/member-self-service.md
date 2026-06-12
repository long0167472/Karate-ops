---
name: member-self-service
description: Member portal — self-service club profile, fee summary, attendance history, and leave requests via /me endpoints
type: feature
version: "1.0"
last_updated: "2026-06-11"
criticality: MEDIUM
metadata:
  owner: backend
  affected_services: [karate-tournament-backend, karate-ops-fe]
  affected_domains: [clubs, attendance, fees]
source_references:
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/web/MemberSelfController.java, note: "/me endpoints" }
  - { file_path: karate-ops-fe/src/App.tsx, note: "MemberPortalPage at /member" }
  - { file_path: karate-ops-fe/src/features/clubs/, note: "Club feature tabs" }
knowledge_graph_refs:
  - { community: "club-operations", hub_node: "OrganizationMember" }
related_context_files:
  - context/features/training-attendance.md
  - context/features/club-finance-fees.md
  - context/features/auth-account.md
---

# Feature: Member Self-Service Portal

## Actors
- **Authenticated Member (MEMBER role)** — all /me endpoints; available to any logged-in user

## Frontend Route
`/member` → `MemberPortalPage` — tabbed interface with: Profile, Fees, Attendance, Leave Requests

## Endpoints (`/api/me`)

| Method | Path | Response | Description |
|--------|------|----------|-------------|
| GET | `/api/me/club-profile` | `MemberClubProfileResponse` | Member's own club profile |
| GET | `/api/me/fees` | `MemberFeeSummaryResponse` | Member's fee assignments and payment status |
| GET | `/api/me/attendance` | `MemberAttendanceSummaryResponse` | Member's attendance records and stats |
| POST | `/api/me/attendance/leave-requests` | `LeaveRequestResponse` (201) | Submit leave request |

Auth: any logged-in user (MEMBER role always present). The service resolves the current actor's `OrganizationMember` via their `userId` + `primaryOrganizationId`.

---

## Flow 1: Club Profile

`GET /api/me/club-profile`

Returns the member's profile in their primary organization:
- `OrganizationMember` record (role, status, joinedAt, payment notes)
- `Person` details (displayName, phone, birthDate, gender, address)
- `AppUser` details (email, username)
- `Organization` summary (name, code)
- Fee roles assigned to the member

**Business rule**: Member can only see their own profile in their `primaryOrganizationId`. If `primaryOrganizationId` is null, returns `ResourceNotFoundException`.

---

## Flow 2: My Fees

`GET /api/me/fees`

Returns:
- All `MemberFeeAssignment` records for the member (ordered by dueDate DESC)
- Summary totals: totalDue, totalPaid, outstanding, waived
- Current fee roles

**MemberFeeSummaryResponse fields** (approximate — confirm exact from source):
```
assignments: [
  {
    feeItemName, billingCycle, amountDue, paidAmount,
    status (PENDING|PARTIAL|PAID|WAIVED|OVERDUE|CANCELLED),
    dueDate, note
  }
],
totalDue, totalPaid, outstanding, totalWaived
```

---

## Flow 3: My Attendance

`GET /api/me/attendance`

Returns attendance records linked to the member (via `organizationMemberId` in `AttendanceRecord`):
- List of sessions the member was recorded in
- Per-session: `status` (PRESENT/ABSENT/LATE/EXCUSED), `checkInAt`
- Summary stats: totalSessions, presentCount, absentCount, lateCount, excusedCount

Members with `attendanceViewEnabled = false` on their `OrganizationMember` cannot access this endpoint — [LOW CONFIDENCE on enforcement].

---

## Flow 4: Leave Request Submission

`POST /api/me/attendance/leave-requests`

```json
{ "sessionId": "uuid", "reason": "family trip" }
```

**Creates**: `AttendanceLeaveRequest { status=PENDING, requesterUser=currentUser, session, reason }`

**Business rules**:
- `sessionId` must exist and belong to member's organization
- Session should be in the future or current (no guard confirmed — may allow past-session requests)
- `reason` required (@NotBlank)

**Decision by coach/manager**:
```
PATCH /api/attendance-leave-requests/{requestId}/decision
{ status: APPROVED|REJECTED, decisionNote? }
```

**When APPROVED**: Does NOT automatically set attendance record to EXCUSED — coach must update manually.

---

## View As (GLOBAL_ADMIN feature)

GLOBAL_ADMIN can use a "View as" role selector in the FE to emulate CLUB_MANAGER or MEMBER perspective. This is a frontend-only UI mechanism — it changes which pages/tabs render, not what the API returns. The actual JWT still contains GLOBAL_ADMIN roles.

## Member Portal FE Architecture

`/member` → `MemberPortalPage` (separate file: `src/features/clubs/` or inline in `App.tsx`).

The portal tabs are rendered by feature components in `src/features/clubs/`:
- `MembersTab` — member list (manager view)
- `AttendanceTab` — session and record management
- `FeesTab` — fee overview and assignment management
- `LeaveRequestsTab` — leave request review

These tabs are for **club manager view** (managing all members). The `/me` endpoints power the **member's own view** of their data.

## Edge Cases

| Scenario | Behavior |
|----------|----------|
| Member has no primaryOrganizationId | `ResourceNotFoundException` on all /me endpoints |
| OrganizationMember not found for user | `ResourceNotFoundException` |
| Leave request for session in wrong org | `ResourceNotFoundException` or `ForbiddenException` |
| Duplicate leave request for same session | [LOW CONFIDENCE] likely `BusinessConflictException` |
