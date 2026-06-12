---
name: person-club-entities
description: Entity schema for Person, Organization, OrganizationMember, Athlete, ClubRoster — the identity and membership chain
type: data-model
version: "1.0"
last_updated: "2026-06-11"
metadata:
  owner: backend
source_references:
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/entity/Person.java }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/entity/Organization.java }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/entity/OrganizationMember.java }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/entity/Athlete.java }
knowledge_graph_refs:
  - { community: "identity", hub_node: "Person" }
  - { community: "club-operations", hub_node: "Organization" }
related_context_files:
  - context/features/club-roster-membership.md
  - context/data-models/auth-domain-entities.md
  - context/data-models/fee-entities.md
---

# Data Model: Person & Club Entities

## Domain Invariant Chain

```
Person ──→ OrganizationMember (person + org + role)
  │              └──→ AppUser (nullable — login account)
  └──→ Athlete (person + sport metadata)
              └──→ ClubRoster (org + athlete enrollment)
```

---

## Organization

**Table**: `organizations`

| Field | Type | Nullable | Default | Notes |
|-------|------|----------|---------|-------|
| `name` | String(180) | No | — | |
| `shortName` | String(80) | Yes | — | |
| `code` | String(60) | Yes | — | Unique lookup code (e.g., `"KARATE_OPS_ADMIN"`) |
| `type` | OrganizationType | No | `CLUB` | Enum |
| `status` | OrganizationStatus | No | `ACTIVE` | Enum |
| `country` | String(80) | Yes | — | |
| `province` | String(120) | Yes | — | |
| `address` | String(255) | Yes | — | |
| `contactEmail` | String(160) | Yes | — | |
| `contactPhone` | String(60) | Yes | — | |

**OrganizationType**: `CLUB, DELEGATION, FEDERATION, ORGANIZER, SCHOOL, OTHER`
**OrganizationStatus**: `ACTIVE, INACTIVE`

Admin org fixed UUID: `00000000-0000-0000-0000-000000000201`

---

## Person

**Table**: `persons`

| Field | Type | Nullable | Notes |
|-------|------|----------|-------|
| `displayName` | String(180) | No | Full name for display |
| `firstName` | String(90) | Yes | |
| `lastName` | String(90) | Yes | |
| `birthDate` | LocalDate | Yes | Used for age category validation |
| `gender` | PersonGender | Yes | `MALE, FEMALE, MIXED, OPEN` |
| `nationalId` | String(80) | Yes | **PII** — national ID / CCCD |
| `email` | String(180) | Yes | **PII** — contact email (may differ from AppUser.email) |
| `phone` | String(60) | Yes | **PII** |
| `currentAddress` | String(255) | Yes | **PII** |
| `emergencyContactName` | String(180) | Yes | |
| `emergencyContactPhone` | String(60) | Yes | |

**PII note**: `nationalId`, `email`, `phone`, `currentAddress` are personally identifiable. Do not log or expose in error messages.

---

## OrganizationMember

**Table**: `organization_members`

| Field | Type | Nullable | Default | Notes |
|-------|------|----------|---------|-------|
| `organization` | Organization | No | — | FK EAGER |
| `user` | AppUser | Yes | — | FK EAGER; nullable — member may have no login |
| `person` | Person | Yes | — | FK EAGER; nullable — may not yet have Person |
| `role` | ClubMemberRole | No | `ATHLETE` | Column: `role_code` |
| `status` | ClubMemberStatus | No | `ACTIVE` | |
| `joinedAt` | LocalDate | Yes | — | |
| `student` | boolean | No | false | Affects fee resolution |
| `attendanceViewEnabled` | boolean | No | true | Can member view own attendance? |
| `tuitionStatus` | PaymentStatus | No | `PENDING` | Denormalized summary |
| `tuitionPaidAmount` | BigDecimal | No | 0 | Denormalized running total |
| `otherFeeStatus` | PaymentStatus | No | `PENDING` | |
| `otherFeePaidAmount` | BigDecimal | No | 0 | |
| `paymentNote` | String(500) | Yes | — | Manager note on payment |
| `memberNote` | String(500) | Yes | — | General manager note |

**ClubMemberRole**: `OWNER, MANAGER, COACH, ATHLETE, PARENT, STAFF`
**ClubMemberStatus**: `PENDING, ACTIVE, INACTIVE, LEFT, SUSPENDED`
**PaymentStatus**: `PAID, PENDING, OVERDUE, WAIVED, PARTIAL`

**Note**: `tuitionStatus` / `tuitionPaidAmount` are denormalized — manually updated by manager or via assignment updates. They do not auto-sync from `MemberFeeAssignment` records.

---

## Athlete

**Table**: `athletes`

| Field | Type | Nullable | Default | Notes |
|-------|------|----------|---------|-------|
| `person` | Person | No | — | @OneToOne EAGER; unique (person can be athlete once) |
| `primaryOrganization` | Organization | Yes | — | Home club; nullable |
| `externalCode` | String(80) | Yes | — | Federation registration number |
| `belt` | String(80) | Yes | — | Free text (no enum): "Dan 1", "Đai nâu" |
| `weightKg` | BigDecimal(6,2) | Yes | — | |
| `heightCm` | BigDecimal(6,2) | Yes | — | |
| `status` | AthleteStatus | No | `ACTIVE` | |

**AthleteStatus**: `ACTIVE, INACTIVE, SUSPENDED`

Unique constraint: `person_id` — one athlete record per person.

---

## ClubRoster

**Table**: `club_roster`

| Field | Type | Nullable | Default | Notes |
|-------|------|----------|---------|-------|
| `organization` | Organization | No | — | FK EAGER |
| `athlete` | Athlete | No | — | FK EAGER |
| `status` | ClubRosterStatus | No | `ACTIVE` | |
| `joinedAt` | LocalDate | Yes | — | |

**ClubRosterStatus**: `ACTIVE, INACTIVE, INJURED, SUSPENDED`

Unique constraint: `(organization_id, athlete_id)` WHERE `deleted_at IS NULL`.

---

## ClubTrainingSchedule

**Table**: `club_training_schedules`

| Field | Type | Nullable | Default | Notes |
|-------|------|----------|---------|-------|
| `organization` | Organization | No | — | FK EAGER |
| `daysOfWeek` | String | No | — | Comma-separated day names: `"MONDAY,WEDNESDAY,FRIDAY"` |
| `startTime` | LocalTime | No | `18:30` | |
| `durationMinutes` | int | No | `90` | |
| `timezone` | String | No | `"Asia/Ho_Chi_Minh"` | No multi-timezone support |
| `active` | boolean | No | true | Inactive schedules skipped by job |

---

## Relationships Diagram

```
Organization ◄────────── AppUser.primaryOrganization
     │
     ├── OrganizationMember ──→ Person
     │         └──→ AppUser (nullable)
     │
     ├── ClubRoster ──→ Athlete ──→ Person
     │
     └── ClubTrainingSchedule ──→ AttendanceSession (generated)
```
