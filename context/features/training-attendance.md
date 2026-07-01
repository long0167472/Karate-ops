---
name: training-attendance
description: Training session creation, attendance marking, leave request workflow, and auto-schedule generation
type: feature
version: "1.0"
last_updated: "2026-06-30"
criticality: HIGH
metadata:
  owner: backend
  affected_services: [karate-tournament-backend, karate-ops-fe]
  affected_domains: [clubs, attendance]
source_references:
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/web/AttendanceController.java, line_range: "1-end", note: "Session CRUD, record marking" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/web/AttendanceLeaveRequestController.java, line_range: "1-end", note: "Leave request list, decide" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/job/ClubTrainingScheduleJob.java, line_range: "1-end", note: "Daily auto-session generator" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/entity/AttendanceSession.java, line_range: "1-end", note: "Session entity" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/entity/AttendanceRecord.java, line_range: "1-end", note: "Record entity with unique constraints" }
knowledge_graph_refs:
  - { community: "club-operations", hub_node: "Organization" }
related_context_files:
  - context/features/club-roster-membership.md
  - context/data-models/attendance-entities.md
  - context/services/attendance-fees-module.md
---

# Feature: Training Attendance

## 2026-06-30 Updates

These notes supersede any older conflicting text below.

- Leave requests now use a discriminated contract:
  - `requestType = LEAVE_SESSION | LEAVE_LONG_TERM | LATE`
  - `sessionId` is required for `LEAVE_SESSION` and `LATE`
  - `fromDate` + `toDate` are required for `LEAVE_LONG_TERM`
  - `status = PENDING | APPROVED | REJECTED | EXPIRED_AUTO_ABSENT`
  - `expiresAt` is returned in `LeaveRequestResponse`
- Manager-side leave APIs are:
  - `GET /api/organizations/{orgId}/attendance-leave-requests`
  - `PATCH /api/attendance-leave-requests/{requestId}/decision`
- Approved leave now auto-applies attendance:
  - `LEAVE_SESSION` and `LEAVE_LONG_TERM` upsert `EXCUSED`
  - `LATE` upsert `LATE`
  - existing `PRESENT`, `LATE`, and `EXCUSED` records are not overwritten; only missing / `ABSENT` records are replaced
- Approved long-term leave is now a source of truth for future sessions:
  - when a manual or scheduled session is created inside the approved date range, the system materializes the attendance record automatically
  - this is triggered from both `AttendanceServiceImpl` and `ClubTrainingScheduleServiceImpl`
- Pending leave expiry is automated by `AttendanceLeaveRequestExpiryJob`:
  - session/late requests expire after `session.scheduledAt`
  - long-term requests expire after end-of-day `toDate`
  - expired requests move to `EXPIRED_AUTO_ABSENT` and materialize `ABSENT` where applicable
- `MemberSelfService.attendance()` now enforces `attendanceViewEnabled`; if the user has no visible membership, the endpoint returns 403 with a domain-specific message.
- `MemberSelfService.attendance()` also returns a separate `leaveRequests` list so pending/approved long-term requests show up even before any in-range session exists.

## Actors
- **Coach / Club Manager** — creates sessions, marks attendance, decides leave requests
- **Member (MEMBER role)** — submits leave requests, views own attendance via `/me`
- **ClubTrainingScheduleJob** — auto-creates sessions at 00:05 daily

## Flow 1: Auto-Session Generation

**Trigger**: `ClubTrainingScheduleJob` runs at cron `0 5 0 * * *` (00:05 Asia/Ho_Chi_Minh) AND on `ApplicationReadyEvent` (server startup).

**Steps**:
1. Load all active `ClubTrainingSchedule` records (all organizations)
2. For each schedule where `daysOfWeek` contains today's day-of-week
3. Call `ClubTrainingScheduleService.ensureTodaySessions(schedule)`
4. Creates `AttendanceSession { type=TRAINING, status=OPEN, source=SCHEDULED, scheduledDate=today, trainingSchedule=schedule }`

**Also available manually**: `POST /organizations/{id}/training-schedule/ensure-today` — same logic, triggered by UI.

**ClubTrainingSchedule fields used**:
- `daysOfWeek` — comma-separated day names (e.g., `"MONDAY,WEDNESDAY,FRIDAY"`)
- `startTime` — LocalTime (default 18:30)
- `durationMinutes` — default 90
- `timezone` — default `"Asia/Ho_Chi_Minh"`
- `active` — boolean; inactive schedules are skipped

**Idempotency**: `ensureSessionForDate()` queries for an existing session with `(trainingSchedule, source=SCHEDULED, scheduledDate=today)` before creating. If one exists, it returns 0 without creating a duplicate. Safe for multiple calls per day.

## Flow 2: Manual Session Creation

**Trigger**: Coach creates a session manually.

```
POST /organizations/{orgId}/attendance-sessions
{ name, type, scheduledAt?, notes? }
```

Session `source = MANUAL`. Session `status = OPEN` by default.

**Session types** (`AttendanceSessionType`): `TRAINING, FITNESS_TEST, WEIGH_IN, TOURNAMENT_CHECKIN`

## Flow 3: Mark Attendance

**Trigger**: Coach marks attendance during or after a session.

```
POST /attendance-sessions/{sessionId}/records
{ organizationMemberId?, athleteId?, status, checkInAt?, note? }
```

**Business rules**:
- At least one of `organizationMemberId` or `athleteId` must be provided (DB CHECK constraint)
- Unique constraint: `(attendance_session_id, organization_member_id)` WHERE `deleted_at IS NULL`
- Unique constraint: `(attendance_session_id, athlete_id)` WHERE `deleted_at IS NULL`
- Attempting to mark the same member twice → DB constraint violation (returned as 409)

**Record status options** (`AttendanceRecordStatus`): `PRESENT, ABSENT, LATE, EXCUSED`

Update an existing record:
```
PATCH /attendance-sessions/{sessionId}/records/{recordId}
{ status?, note? }
```

## Flow 4: Leave Request

**Trigger**: Member submits leave request for an upcoming session.

**Step A — Member submits (MEMBER role)**:
```
POST /me/attendance/leave-requests
{ sessionId, reason }
```
Creates `AttendanceLeaveRequest { status=PENDING, requesterUser=currentUser }`.

**Step B — Coach or Manager decides**:
```
PATCH /attendance-leave-requests/{requestId}/decision
{ status: APPROVED|REJECTED, decisionNote? }
```
Sets `status`, `decidedAt = Instant.now()`, `decidedByUser = currentUser`.

**When APPROVED**: Typically the attendance record for that member should be set to `EXCUSED`, but this linkage is not automatic — coach must manually update the record.

## Flow 5: Day-Off

**Trigger**: No training on a normally scheduled day.

```
POST /organizations/{id}/training-schedule/day-off
{ date? }
```
Marks a specific date as a day-off in the schedule. Any auto-generated session for that date is cancelled or skipped.

## Edge Cases

| Scenario | Behavior |
|----------|----------|
| Session already CLOSED | Cannot add new records (status check in service) |
| Mark attendance for non-member athlete | Allowed if `athleteId` provided directly |
| Leave request for already-decided session | [LOW CONFIDENCE] likely allowed (no explicit guard seen) |
| Schedule `daysOfWeek = ""` (empty) | No sessions generated — schedule is effectively inactive |

## Regional Variants
No regional variants. Vietnamese timezone `Asia/Ho_Chi_Minh` is hardcoded in `ClubTrainingSchedule.timezone` default. No multi-timezone support.
