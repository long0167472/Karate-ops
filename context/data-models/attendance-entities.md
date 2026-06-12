---
name: attendance-entities
description: Entity schema for AttendanceSession, AttendanceRecord, AttendanceLeaveRequest, ClubTrainingSchedule
type: data-model
version: "1.0"
last_updated: "2026-06-11"
metadata:
  owner: backend
source_references:
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/entity/AttendanceSession.java }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/entity/AttendanceRecord.java }
knowledge_graph_refs:
  - { community: "club-operations", hub_node: "Organization" }
related_context_files:
  - context/features/training-attendance.md
  - context/data-models/person-club-entities.md
---

# Data Model: Attendance Entities

## AttendanceSession

**Table**: `attendance_sessions`

| Field | Type | Nullable | Default | Notes |
|-------|------|----------|---------|-------|
| `organization` | Organization | No | — | FK EAGER |
| `tournamentParticipant` | TournamentParticipant | Yes | — | FK EAGER; set for TOURNAMENT_CHECKIN type |
| `trainingSchedule` | ClubTrainingSchedule | Yes | — | FK EAGER; set when source=SCHEDULED |
| `name` | String(180) | No | — | |
| `type` | AttendanceSessionType | No | `TRAINING` | Enum |
| `status` | AttendanceSessionStatus | No | `OPEN` | Enum |
| `scheduledAt` | Instant | Yes | — | Full timestamp (legacy); prefer `scheduledDate` |
| `scheduledDate` | LocalDate | Yes | — | Date of session (set by job for SCHEDULED) |
| `source` | AttendanceSessionSource | No | `MANUAL` | Enum |
| `notes` | String(500) | Yes | — | |

**AttendanceSessionType**: `TRAINING, FITNESS_TEST, WEIGH_IN, TOURNAMENT_CHECKIN, OTHER`
**AttendanceSessionStatus**: `SCHEDULED, OPEN, CLOSED, CANCELLED`
**AttendanceSessionSource**: `MANUAL, SCHEDULED`

Sessions created by `ClubTrainingScheduleJob` have:
- `source = SCHEDULED`
- `scheduledDate = today`
- `trainingSchedule = <the schedule that triggered it>`
- `status = OPEN`

---

## AttendanceRecord

**Table**: `attendance_records`

| Field | Type | Nullable | Default | Notes |
|-------|------|----------|---------|-------|
| `session` | AttendanceSession | No | — | FK EAGER |
| `organizationMember` | OrganizationMember | Yes | — | FK EAGER; one must be provided |
| `athlete` | Athlete | Yes | — | FK EAGER; alternative to organizationMember |
| `status` | AttendanceRecordStatus | No | `PRESENT` | Enum |
| `checkInAt` | Instant | Yes | — | Actual check-in timestamp |
| `note` | String(500) | Yes | — | |

**AttendanceRecordStatus**: `PRESENT, ABSENT, LATE, EXCUSED`

**DB-level constraints**:
- CHECK: `organization_member_id IS NOT NULL OR athlete_id IS NOT NULL`
- UNIQUE: `(attendance_session_id, organization_member_id)` WHERE `deleted_at IS NULL`
- UNIQUE: `(attendance_session_id, athlete_id)` WHERE `deleted_at IS NULL`

Duplicate mark → DB unique constraint violation → 409.

---

## AttendanceLeaveRequest

**Table**: `attendance_leave_requests`

| Field | Type | Nullable | Notes |
|-------|------|----------|-------|
| `session` | AttendanceSession | No | FK — which session the leave is for |
| `requesterUser` | AppUser | No | FK — who submitted |
| `reason` | String | No | @NotBlank |
| `status` | String | No | `"PENDING"`, `"APPROVED"`, `"REJECTED"` |
| `decisionNote` | String | Yes | Manager's response |
| `decidedAt` | Instant | Yes | |
| `decidedByUser` | AppUser | Yes | FK — who decided |

Member submits via `POST /api/me/attendance/leave-requests`.
Manager decides via `PATCH /api/attendance-leave-requests/{requestId}/decision`.

**Approval is not auto-linked** to AttendanceRecord — coach must manually set record to EXCUSED after approving.

---

## ClubTrainingSchedule

**Table**: `club_training_schedules`

| Field | Type | Nullable | Default | Notes |
|-------|------|----------|---------|-------|
| `organization` | Organization | No | — | FK EAGER |
| `daysOfWeek` | String | No | — | Comma-separated: `"MONDAY,WEDNESDAY,FRIDAY"` |
| `startTime` | LocalTime | No | `18:30` | |
| `durationMinutes` | int | No | `90` | |
| `timezone` | String | No | `"Asia/Ho_Chi_Minh"` | |
| `active` | boolean | No | true | Inactive schedules skipped by job |

**Job behavior** (`ClubTrainingScheduleJob`):
- Cron: `0 5 0 * * *` (00:05 daily)
- Also runs on `ApplicationReadyEvent` (server startup)
- For each active schedule, if today is in `daysOfWeek`, calls `ensureTodaySessions()`
- No idempotency guard — double-run risk on restart. See [known-issues/patterns.md Pattern 5].

---

## Relationships

```
Organization
  └── ClubTrainingSchedule (recurring schedule)
        └── AttendanceSession (generated per day, source=SCHEDULED)
              └── AttendanceRecord (per-member mark)
              └── AttendanceLeaveRequest (member leave application)

Organization
  └── AttendanceSession (manual, source=MANUAL)
        └── AttendanceRecord
```
