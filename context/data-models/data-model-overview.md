---
name: data-model-overview
description: High-level entity relationship map, god nodes, soft-delete pattern, and all enum values across the domain
type: data-model
version: "1.0"
last_updated: "2026-06-11"
metadata:
  owner: backend
source_references:
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/entity/BaseEntity.java }
knowledge_graph_refs:
  - { community: "all", hub_node: "Organization" }
related_context_files:
  - context/data-models/auth-domain-entities.md
  - context/data-models/person-club-entities.md
  - context/data-models/attendance-entities.md
  - context/data-models/fee-entities.md
  - context/data-models/tournament-entities.md
  - context/data-models/match-entities.md
---

# Data Model Overview

## BaseEntity (All entities inherit)

```java
@MappedSuperclass
public abstract class BaseEntity {
    @Id UUID id;                  // set by @PrePersist if null
    Instant createdAt;            // not updatable
    Instant updatedAt;
    Instant deletedAt;            // null = alive; non-null = soft-deleted
}
```

**Soft delete**: `entity.softDelete()` sets `deletedAt = Instant.now()`. Repositories should filter `WHERE deleted_at IS NULL`. Never use physical DELETE.

---

## Domain Groups & Entity List

### Auth / Identity
| Entity | Table | Notes |
|--------|-------|-------|
| `AppUser` | `users` | Login credential holder; references `Organization` for primary org |
| `Role` | `roles` | System role catalog (one per `SystemRole` enum value) |
| `UserRoleAssignment` | `user_role_assignments` | `(user, role, scopeType, scopeId)` — scoped RBAC |
| `AccountRequest` | `account_requests` | Membership signup waiting for club approval |

### Person / Club
| Entity | Table | Notes |
|--------|-------|-------|
| `Person` | `persons` | PII identity root (name, DOB, nationalId, contact) |
| `Organization` | `organizations` | Club, federation, school, or organizer |
| `OrganizationMember` | `organization_members` | `(person, user, org)` membership with role + payment status |
| `Athlete` | `athletes` | `Person` with sport metadata (belt, weight, external code) |
| `ClubRoster` | `club_roster` | `(org, athlete)` enrollment for tournament eligibility |
| `ClubTrainingSchedule` | `club_training_schedules` | Recurring schedule (daysOfWeek, startTime, timezone) |

### Attendance
| Entity | Table | Notes |
|--------|-------|-------|
| `AttendanceSession` | `attendance_sessions` | Single training or event session (MANUAL or SCHEDULED) |
| `AttendanceRecord` | `attendance_records` | Per-member attendance mark for a session |
| `AttendanceLeaveRequest` | `attendance_leave_requests` | Member leave request → approval workflow |

### Fees / Finance
| Entity | Table | Notes |
|--------|-------|-------|
| `ClubFeeRole` | `club_fee_roles` | Pricing tier (NORMAL, STUDENT, ATHLETE, etc.) |
| `OrganizationMemberFeeRole` | `organization_member_fee_roles` | Member → fee role assignments |
| `ClubFeeItem` | `club_fee_items` | Fee product (tuition, one-time payment) |
| `ClubFeeItemRoleAmount` | `club_fee_item_role_amounts` | Per-role price for a fee item; `exempt=true` waives |
| `MemberFeeAssignment` | `member_fee_assignments` | Per-member fee charge with payment tracking |
| `OrganizationMemberTuitionOverride` | `org_member_tuition_overrides` | Per-member tuition override (replaces default item) |
| `ClubFinanceExpense` | `club_finance_expenses` | Club expenditure tracking |

### Tournament
| Entity | Table | Notes |
|--------|-------|-------|
| `Tournament` | `tournaments` | Tournament event shell |
| `TournamentParticipant` | `tournament_participants` | `(tournament, org)` delegation — must be APPROVED before entries |
| `Category` | `categories` | Competition event (Kumite -60kg, Kata Women, etc.) |
| `Entry` | `entries` | `(category, participant, athlete)` registration |
| `CategoryResult` | `category_results` | Final medal record per entry |

### Match
| Entity | Table | Notes |
|--------|-------|-------|
| `Bracket` | `brackets` | `(category, type, size)` bracket shell |
| `Match` | `matches` | Individual match with status, winner, bracket position |
| `MatchParticipant` | `match_participants` | `(match, entry, side)` — AKA or AO corner |
| `KumiteMatchState` | `kumite_match_state` | Live score/timer state, @MapsId on match |
| `KataVote` | `kata_votes` | Per-judge vote for kata match |
| `MatchScoreEvent` | `match_score_events` | Event log per scoring action |
| `MatchAuditEvent` | `match_audit_events` | Audit trail for result confirmation |
| `Tatami` | `tatamis` | `(tournament, tatamiNo)` physical mat area |

---

## God Nodes (Highest FK Fan-In)

| Entity | Referenced by |
|--------|--------------|
| `Organization` | AppUser, OrganizationMember, Athlete, ClubFeeItem, ClubFeeRole, MemberFeeAssignment, Tournament, TournamentParticipant, AttendanceSession, ClubRoster, ClubTrainingSchedule |
| `AppUser` | OrganizationMember, Tournament, AccountRequest, UserRoleAssignment, MatchAuditEvent |
| `Person` | OrganizationMember, Athlete, AccountRequest |

---

## Domain Invariant Chain

```
Person → OrganizationMember → Athlete → ClubRoster → TournamentParticipant → Entry → MatchParticipant
```
Cannot skip steps. See [person-club-entities.md](./person-club-entities.md) and [tournament-entities.md](./tournament-entities.md).

---

## All Enums Reference

### Identity / Auth
- **OrganizationType**: `CLUB, DELEGATION, FEDERATION, ORGANIZER, SCHOOL, OTHER`
- **OrganizationStatus**: `ACTIVE, INACTIVE`

### Person / Club
- **PersonGender**: `MALE, FEMALE, MIXED, OPEN`
- **AthleteStatus**: `ACTIVE, INACTIVE, SUSPENDED`
- **ClubMemberRole**: `OWNER, MANAGER, COACH, ATHLETE, PARENT, STAFF`
- **ClubMemberStatus**: `PENDING, ACTIVE, INACTIVE, LEFT, SUSPENDED`
- **ClubRosterStatus**: `ACTIVE, INACTIVE, INJURED, SUSPENDED`

### Fees / Finance
- **PaymentStatus**: `PAID, PENDING, OVERDUE, WAIVED, PARTIAL`
- **BillingCycle**: `ONE_TIME, MONTHLY, QUARTERLY, YEARLY`
- **FeeItemType**: `TUITION, UNIFORM, EXAM, TOURNAMENT, OTHER`
- **FeeItemKind**: `MONTHLY_TUITION_DEFAULT, MONTHLY_TUITION_OVERRIDE, ONE_TIME_INCOME`
- **FeeItemStatus**: `DRAFT, ACTIVE, ARCHIVED`
- **FeeAssignmentSource**: `RULE, MANUAL`
- **MemberFeeAssignmentStatus**: `PENDING, PARTIAL, PAID, WAIVED, OVERDUE, CANCELLED`

### Attendance
- **AttendanceSessionType**: `TRAINING, FITNESS_TEST, WEIGH_IN, TOURNAMENT_CHECKIN, OTHER`
- **AttendanceSessionStatus**: `SCHEDULED, OPEN, CLOSED, CANCELLED`
- **AttendanceSessionSource**: `MANUAL, SCHEDULED`
- **AttendanceRecordStatus**: `PRESENT, ABSENT, LATE, EXCUSED`

### Tournament
- **TournamentVisibility**: `PUBLIC, PRIVATE, INVITE_ONLY`
- **TournamentStatus**: `DRAFT, REGISTRATION_OPEN, REGISTRATION_CLOSED, DRAWING, RUNNING, COMPLETED, ARCHIVED`
- **RulesetVersion**: `WKF_2026, LOCAL`
- **RulesetPreset**: `WKF, PHONG_TRAO, NANG_CAO, CUSTOM`
- **CategoryDiscipline**: `KUMITE, KATA, PARA_KATA, TEAM_KUMITE, TEAM_KATA`
- **CompetitionLevel**: `PHONG_TRAO, NANG_CAO, OPEN`
- **EntryType**: `INDIVIDUAL, TEAM`
- **ParticipantStatus**: `INVITED, REQUESTED, APPROVED, REJECTED, WITHDRAWN`
- **EntryStatus**: `REGISTERED, CHECKED_IN, WITHDRAWN, DISQUALIFIED`
- **WeighInStatus**: `VALID, MISSING_WEIGHT, OUT_OF_CLASS, NEEDS_ORGANIZER_REVIEW`
- **BracketType**: `SINGLE_ELIMINATION, REPECHAGE, ROUND_ROBIN, POOL`
- **BracketStatus**: `DRAFT, GENERATED, LOCKED`

### Match
- **MatchStatus**: `SCHEDULED, READY, RUNNING, PAUSED, REVIEW, HANTEI, VOTING, COMPLETED, LOCKED, CANCELLED`
- **WinType**: `IPPON, SHIDO, HANTEI, KIKEN, SHIKKAKU, FUSEN, HANSOKU, DISQUALIFIED, MANUAL, BYE`
- **Side**: `AKA, AO`

### Auth / RBAC
- **SystemRole**: `GLOBAL_ADMIN, CLUB_MANAGER, TOURNAMENT_OWNER, COACH, TATAMI_OPERATOR, JUDGE, VIEWER, MEMBER`
