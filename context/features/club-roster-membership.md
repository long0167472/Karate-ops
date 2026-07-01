---
name: club-roster-membership
description: Club member management, athlete roster, person identity, and the domain invariant chain
type: feature
version: "1.0"
last_updated: "2026-06-29"
criticality: HIGH
metadata:
  owner: backend
  affected_services: [karate-tournament-backend, karate-ops-fe]
  affected_domains: [clubs, tournaments]
source_references:
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/web/ClubMemberController.java, line_range: "1-end", note: "Member CRUD under /organizations/{id}/members" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/web/ClubRosterController.java, line_range: "1-end", note: "Roster CRUD under /organizations/{id}/roster" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/web/AthleteController.java, line_range: "1-end", note: "Athlete CRUD" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/web/PersonController.java, line_range: "1-end", note: "Person CRUD" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/entity/OrganizationMember.java, line_range: "1-end", note: "Member entity — both person_id and user_id" }
knowledge_graph_refs:
  - { community: "club-operations", hub_node: "Organization" }
  - { community: "identity", hub_node: "Person" }
related_context_files:
  - context/features/auth-account.md
  - context/features/club-finance-fees.md
  - context/data-models/person-club-entities.md
---

# Feature: Club Roster & Membership

## 2026-06-29 Updates

These notes supersede any older conflicting text below.

- `GET /api/organizations`, `GET /api/persons`, and `GET /api/athletes` are now admin-only.
- Club-management FE should use org-scoped APIs:
  - `GET /api/organizations/managed-clubs`
  - `GET|POST|PATCH /api/organizations/{orgId}/athletes[...]`
  - `PATCH /api/organizations/{orgId}/persons/{personId}`
- Athlete ownership is now **hybrid**:
  - `Athlete.primaryOrganization` remains the owning / profile-editing club.
  - The same athlete may have multiple `ACTIVE` roster rows across different clubs.
  - Secondary clubs may use the athlete for roster, attendance, and tournament flows inside their own org only.
- Editing rules:
  - Global admin may still use the global backoffice endpoints.
  - Club-side person/athlete editing is restricted to the **primary club** only.
  - Reassigning `primaryOrganizationId` requires the athlete to already have an `ACTIVE` roster in the target org.
- `ClubRosterServiceImpl.requireAthleteBelongsToOrganization(...)` now validates **active roster membership**, not `primaryOrganizationId`.
- Removing an `ACTIVE` roster from the current primary club now behaves as follows:
  - if the athlete is still `ACTIVE` in another club, the removal is blocked until primary club is reassigned manually;
  - if no active roster remains anywhere, `primaryOrganizationId` is cleared to `null`.
- `ClubMemberCreateRequest`, `ClubMemberUpdateRequest`, and `MemberAccountCreateRequest` no longer accept finance summary fields such as `tuitionStatus` / `otherFeeStatus`. Those columns are deprecated data only.

## Domain Invariant Chain

```
Person (identity record)
  └─ OrganizationMember (person + org + role + payment status)
       └─ Athlete (person + weight + belt + tournament eligibility)
            └─ ClubRoster (org + athlete + roster status)
                 └─ TournamentParticipant → Entry (tournament-specific)
```

**Rule**: Every entity in the chain requires the one above it. You cannot create an Athlete without a Person. You cannot add an athlete to a ClubRoster without them being an Athlete. Tournament entries require ClubRoster membership (validated via `ClubRosterService`).

## Actors
- **Club Manager (CLUB_MANAGER)** — manages all members, athletes, and roster
- **Global Admin (GLOBAL_ADMIN)** — manages all organizations and persons

## Flow 1: Add Club Member

**Step 1 — Create Person (if not exists)**:
```
POST /api/persons
{ displayName, firstName, lastName, birthDate, gender, nationalId, email, phone, currentAddress, emergencyContactName, emergencyContactPhone }
```
Returns `PersonResponse { id, ... }`.

**Step 2 — Create or link AppUser** (if member needs login):
```
POST /api/organizations/{orgId}/member-accounts
{ personId?, displayName, email, phone, gender, birthDate, ... role, status }
```
If `personId` provided, links existing Person. Otherwise creates new Person + AppUser atomically via `AccountProvisioningServiceImpl`.

**Step 3 — OrganizationMember created automatically** with:
- `role` = `ClubMemberRole.ATHLETE` (default) or specified
- `status` = `ClubMemberStatus.ACTIVE` (default)
- `tuitionStatus` = `PaymentStatus.PENDING`
- `otherFeeStatus` = `PaymentStatus.PENDING`
- `student` = true (default)
- `attendanceViewEnabled` = true (default)
- `joinedAt` = today

**ClubMemberRole values**: `OWNER, MANAGER, COACH, ATHLETE, PARENT, STAFF`
**ClubMemberStatus values**: `PENDING, ACTIVE, INACTIVE, LEFT, SUSPENDED`

## Flow 2: Manage Athlete Profile

An athlete is a Person with tournament metadata:
```
POST /api/athletes
{ personId (required), primaryOrganizationId, externalCode, belt, weightKg, heightCm, status }
```
`Person → Athlete` is a 1:1 `@OneToOne` relationship. A person can only be an athlete once.

**Athlete fields**:
- `belt` — String (e.g., "Dan 1", "Đai nâu") — no enum, free text
- `weightKg` / `heightCm` — BigDecimal (precision 6, scale 2)
- `status` — `AthleteStatus`: `ACTIVE, INACTIVE, SUSPENDED`
- `externalCode` — for federation registration number

## Flow 3: Club Roster Enrollment

An athlete must be enrolled in a club's roster to participate in tournaments hosted by that club's delegation:
```
POST /api/organizations/{orgId}/roster
{ athleteId (required), status, joinedAt }
```

Creates `ClubRoster { organization, athlete, status=ACTIVE, joinedAt=today }`.
Unique constraint: `(organization_id, athlete_id)` — athlete can only appear once per org roster.

**ClubRosterStatus**: `ACTIVE, INACTIVE, INJURED, SUSPENDED`

## Flow 4: Update Member Status / Role

```
PATCH /api/organizations/{orgId}/members/{memberId}
{ role?, status?, paymentNote?, memberNote?, tuitionStatus?, otherFeeStatus?, ... }
```

Partial update — all fields optional. Most commonly used to:
- Change `status` to `INACTIVE` or `LEFT` when member leaves
- Update `tuitionStatus` / `otherFeeStatus` after payment

## Business Rules

1. **Person is the identity root** — always create or find a Person first. Never create an AppUser without a corresponding Person record (enforced in `AccountProvisioningServiceImpl`).

2. **OrganizationMember.user_id is nullable** — not all members have system accounts. Members added by import or provisioned without email may have `user_id = null`. This is valid.

3. **Athlete links to Person, not AppUser** — `Athlete.person` is a `@OneToOne` to `Person`. There is no direct FK from `Athlete` to `AppUser`. Finding the AppUser for an athlete requires: `Athlete.person → OrganizationMember.person → OrganizationMember.user`.

4. **Same athlete, multiple clubs** — a `ClubRoster` entry is per `(organization, athlete)`. The same athlete can appear in multiple organizations' rosters. `Athlete.primaryOrganization` is the "home" club but is nullable.

5. **PersonGender enum** — `MALE, FEMALE, MIXED, OPEN` (note: `MIXED` and `OPEN` exist for edge cases — category gender filtering uses this).

## Edge Cases

| Scenario | Behavior |
|----------|----------|
| Create athlete for person who already has one | DB unique constraint on `person_id` → 409 |
| Add athlete to roster already in roster | DB unique constraint on `(org, athlete)` → 409 |
| Delete person with existing athlete/member records | Soft-delete allowed; does not cascade (orphaned athlete remains) |
| Member with no email | Allowed — `OrganizationMember.person.email` nullable |

## Regional Variants
None. Vietnamese-specific: `OrganizationType.OTHER` and `SCHOOL` exist as org types (V1 migration).
