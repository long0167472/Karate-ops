---
name: auth-domain-entities
description: Entity schema for AppUser, Role, UserRoleAssignment, AccountRequest — auth and RBAC tables
type: data-model
version: "1.0"
last_updated: "2026-06-11"
metadata:
  owner: backend
source_references:
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/entity/AppUser.java }
  - { file_path: karate-tournament-backend/src/main/resources/db/migration/, note: "V1, V2 migrations for users and roles tables" }
knowledge_graph_refs:
  - { community: "auth-security", hub_node: "AppUser" }
related_context_files:
  - context/services/auth-module.md
  - context/features/auth-account.md
  - context/data-models/person-club-entities.md
---

# Data Model: Auth Domain Entities

## AppUser

**Table**: `users`

| Field | Type | Column | Nullable | Default | Notes |
|-------|------|--------|----------|---------|-------|
| `id` | UUID | `id` | No | auto | Inherited from BaseEntity |
| `displayName` | String | `display_name` | No | — | max 180 chars |
| `email` | String | `email` | Yes | — | max 180 chars; normalized lowercase |
| `username` | String | `username` | Yes | — | max 120 chars; generated via UsernameGenerator |
| `phone` | String | `phone` | Yes | — | max 60 chars |
| `status` | String | `status` | No | `"ACTIVE"` | max 40 chars; not an enum — raw string |
| `passwordHash` | String | `password_hash` | Yes | — | max 120; BCrypt encoded |
| `lastLoginAt` | Instant | `last_login_at` | Yes | — | updated on each login |
| `primaryOrganization` | Organization | `primary_organization_id` | Yes | — | @ManyToOne EAGER FK |
| `createdAt` | Instant | `created_at` | No | — | Inherited |
| `updatedAt` | Instant | `updated_at` | No | — | Inherited |
| `deletedAt` | Instant | `deleted_at` | Yes | — | Soft delete |

**Key notes**:
- `status` is a String, not an enum — compare as `"ACTIVE"` string
- `email` and `username` are both searchable login credentials (`findByEmailOrUsername`)
- `passwordHash` is nullable — users seeded without login access may have null
- Global admin has fixed UUID: `00000000-0000-0000-0000-000000000001`

---

## Role

**Table**: `roles`

| Field | Type | Column | Nullable | Notes |
|-------|------|--------|----------|-------|
| `id` | UUID | `id` | No | auto |
| `code` | String | `code` | No | `SystemRole.name()` — e.g., `"GLOBAL_ADMIN"` |
| `name` | String | `name` | No | Human-readable, e.g., `"GLOBAL ADMIN"` |
| `description` | String | `description` | Yes | e.g., `"System role GLOBAL_ADMIN"` |

Auto-seeded on startup by `AuthServiceImpl.ensureRoles()` for all `SystemRole` enum values.

**SystemRole enum**: `GLOBAL_ADMIN, CLUB_MANAGER, TOURNAMENT_OWNER, COACH, TATAMI_OPERATOR, JUDGE, VIEWER, MEMBER`

---

## UserRoleAssignment

**Table**: `user_role_assignments`

| Field | Type | Column | Nullable | Notes |
|-------|------|--------|----------|-------|
| `id` | UUID | `id` | No | auto |
| `user` | AppUser | `user_id` | No | FK @ManyToOne EAGER |
| `role` | Role | `role_id` | No | FK @ManyToOne EAGER |
| `scopeType` | String | `scope_type` | Yes | `"GLOBAL"` or `"ORGANIZATION"` |
| `scopeId` | UUID | `scope_id` | Yes | Organization UUID for org-scoped roles; null for GLOBAL |
| `deletedAt` | Instant | `deleted_at` | Yes | Soft delete |

**RBAC scope examples**:
- GLOBAL_ADMIN: `scopeType="GLOBAL"`, `scopeId=null`
- CLUB_MANAGER for org X: `scopeType="ORGANIZATION"`, `scopeId=orgX.id`

---

## AccountRequest

**Table**: `account_requests`

| Field | Type | Column | Nullable | Notes |
|-------|------|--------|----------|-------|
| `id` | UUID | `id` | No | auto |
| `organization` | Organization | `organization_id` | No | FK — club being requested to join |
| `displayName` | String | `display_name` | No | From self-registration form |
| `email` | String | `email` | No | Unique per (email, org) for PENDING requests |
| `phone` | String | `phone` | Yes | |
| `gender` | PersonGender | `gender` | Yes | |
| `birthDate` | LocalDate | `birth_date` | Yes | |
| `currentAddress` | String | `current_address` | Yes | |
| `status` | String | `status` | No | `"PENDING"`, `"APPROVED"`, `"REJECTED"` |
| `decisionNote` | String | `decision_note` | Yes | Required when status=REJECTED |
| `decidedAt` | Instant | `decided_at` | Yes | |
| `approvedUser` | AppUser | `approved_user_id` | Yes | FK — set when APPROVED, points to provisioned user |

**Transition rule**: `status` can only transition once from `PENDING` → `APPROVED` or `REJECTED`. Re-deciding throws `BusinessConflictException`.

---

## Relationships Diagram

```
AppUser ─────────────────── primaryOrganization ──→ Organization
    │
    └─── UserRoleAssignment(s) ─→ Role (code=SystemRole.name)
    │         scopeType="GLOBAL" | "ORGANIZATION"
    │         scopeId=null | org.id
    │
AccountRequest ──→ Organization (club)
    └── approvedUser ──→ AppUser (after APPROVED)
```
