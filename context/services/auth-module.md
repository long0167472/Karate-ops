---
name: auth-module
description: Auth service internals — JWT creation/verification, role loading, admin seeding, RBAC enforcement
type: service
version: "1.0"
last_updated: "2026-06-11"
criticality: CRITICAL
metadata:
  owner: backend
  affected_services: [karate-tournament-backend]
  affected_domains: [auth]
source_references:
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/service/impl/AuthServiceImpl.java, note: "Login, me, seeding, role management" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/auth/impl/JwtServiceImpl.java, note: "JWT create, verify, sign" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/security/JwtAuthenticationFilter.java, note: "Filter: extract + verify JWT on each request" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/service/impl/PermissionServiceImpl.java, note: "RBAC enforcement" }
knowledge_graph_refs:
  - { community: "auth-security", hub_node: "AppUser" }
related_context_files:
  - context/features/auth-account.md
  - context/client-rules/client-specific-rules.md
  - context/data-models/auth-domain-entities.md
---

# Service: Auth Module

## Responsibilities
1. Login credential validation → JWT issuance
2. JWT verification on every request
3. Startup role seeding + global admin bootstrap
4. RBAC permission enforcement (PermissionService)

## JWT Structure

### Header
```json
{ "alg": "HS256", "typ": "JWT" }
```

### Payload
```json
{
  "sub": "<userId UUID string>",
  "email": "<user email>",
  "name": "<displayName>",
  "primaryOrganizationId": "<org UUID string | null>",
  "roles": ["GLOBAL_ADMIN", "MEMBER"],
  "iat": 1749600000,
  "exp": 1749643200
}
```
- `roles` is always sorted and always contains `"MEMBER"`
- `primaryOrganizationId` is `null` (JSON null) when `AppUser.primaryOrganization` is null
- Claims are ordered via `LinkedHashMap` — field order is deterministic in output

### Signing
- Algorithm: HMAC-SHA256
- Secret: decoded from base64 property `app.security.jwt.secret`
- Token format: `Base64URL(header).Base64URL(payload).Base64URL(HMAC-SHA256(header.payload))`
- All Base64 uses URL-safe encoding without padding

### Verification (JwtServiceImpl.verify())
1. Split token by `"."` — must have exactly 3 parts
2. Recompute signature of `parts[0].parts[1]` via `sign()`
3. Constant-time comparison of computed vs `parts[2]`
4. Decode `parts[1]` payload JSON
5. Check `Instant.now().getEpochSecond() >= exp` → `UnauthorizedException("Token expired")`
6. Extract sub, email, name, primaryOrganizationId, roles
7. Add MEMBER to roles set
8. Return `AuthenticatedPrincipal`
9. Any parsing/signing error → `UnauthorizedException("Invalid token")`

**Security note**: Uses constant-time comparison (`constantTimeEquals`) to prevent timing attacks on HMAC verification.

## Request Auth Flow (JwtAuthenticationFilter)

On every HTTP request (except public routes):
1. Extract `Authorization: Bearer <token>` header
2. Call `jwtService.verify(token)` → `AuthenticatedPrincipal`
3. Wrap in `JwtAuthenticationToken` and put in `SecurityContextHolder`
4. If verification fails → filter clears context and request proceeds unauthenticated (403 from method security)

Services receive the actor via `CurrentActorProvider.get()` (injected, implemented by `JwtCurrentActorProvider`).

## Role System

### SystemRole enum
All roles: `GLOBAL_ADMIN, CLUB_MANAGER, TOURNAMENT_OWNER, COACH, TATAMI_OPERATOR, JUDGE, VIEWER, MEMBER`

### Stored in DB
- `roles` table — one row per `SystemRole` value, `code = SystemRole.name()`
- `user_role_assignments` table — `(user_id, role_id, scope_type, scope_id)`
- `scope_type = "GLOBAL"` for admin assignments; `scope_type = "ORGANIZATION"` + `scope_id = org UUID` for org-scoped roles

### Loaded at login
```
roleAssignments = findByUser_IdAndDeletedAtIsNull(userId)
systemRoles = roleAssignments.stream()
    .map(a -> SystemRole.valueOf(a.role.code))
    .collect(to LinkedHashSet)
systemRoles.add(SystemRole.MEMBER)  // always added
```
Roles are embedded in JWT — **not re-loaded on each request**. If roles change, existing tokens remain valid until expiry.

## RBAC Enforcement (PermissionService)

`PermissionServiceImpl` checks actor roles before service operations:

| Permission Check | Required Role / Scope |
|-----------------|----------------------|
| `requireGlobalAdmin()` | `GLOBAL_ADMIN` |
| `requireTournamentCreate(orgId)` | `GLOBAL_ADMIN` OR `CLUB_MANAGER` with scope = orgId |
| `requireRosterManage(orgId)` | `GLOBAL_ADMIN` OR `CLUB_MANAGER` with scope = orgId |
| `requireClubView(orgId)` | `GLOBAL_ADMIN` OR any org-scoped role with scope = orgId |
| `requireMatchControl(tatamiId)` | `TATAMI_OPERATOR` OR `GLOBAL_ADMIN` |

Failure → `ForbiddenException` (→ HTTP 403)

### CLUB_MANAGER scope enforcement
CLUB_MANAGER can only manage organizations they own — `scope_id = organizationId` in the role assignment. This means `CLUB_MANAGER` without an org scope assignment cannot manage any org.

## Admin Seeding (On Startup)

`AuthServiceImpl` implements `ApplicationRunner` — runs after Spring context is ready.

### Step 1: Ensure all SystemRole rows exist
For each `SystemRole` value, create `Role` record if not present:
- `code = role.name()`
- `name = role.name().replace('_', ' ')`
- `description = "System role " + role.name()`

### Step 2: Seed global admin
**Fixed UUIDs** (do not change — breaking if changed):
- Admin org: `00000000-0000-0000-0000-000000000201`
- Admin user: `00000000-0000-0000-0000-000000000001`

Organization created/updated:
```
name = "Karate Ops Admin"
shortName = "Karate Ops"
code = "KARATE_OPS_ADMIN"
type = ORGANIZER
status = ACTIVE
```

User created/updated:
```
displayName = ${app.security.seed-admin.display-name}
email = ${app.security.seed-admin.email}  (normalized)
username = "global.admin" (only if currently blank)
status = "ACTIVE"
passwordHash = BCrypt(${app.security.seed-admin.password})
primaryOrganization = admin org
```
Then `assignRole(admin, GLOBAL_ADMIN, "GLOBAL", null)`.

**Config properties** (application.yml / env override):
- `app.security.seed-admin.email` → default `admin@karate-ops.local`
- `app.security.seed-admin.password` → default `Admin@123456`
- `app.security.seed-admin.display-name` → default value in config
- `app.security.jwt.secret` → base64 secret
- `app.security.jwt.expires-minutes` → token TTL in minutes
- `app.security.enabled` → `false` in dev profile to skip auth (see client-rules)

## AuthenticatedPrincipal Record

```java
public record AuthenticatedPrincipal(
    UUID userId,
    UUID primaryOrganizationId,   // nullable
    String email,
    String displayName,
    Set<SystemRole> roles
) implements CurrentActor {}
```

Methods: `userId()`, `primaryOrganizationId()`, `email()`, `displayName()`, `roles()`, `hasRole(SystemRole)`, `isGlobalAdmin()`

## Exception Types

| Exception | HTTP | When |
|-----------|------|------|
| `UnauthorizedException` | 401 | Invalid credentials, expired/invalid token |
| `ForbiddenException` | 403 | Insufficient roles or scope |
| `BadRequestException` | 400 | Password too short, registration disabled |
