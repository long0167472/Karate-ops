---
name: auth-account
description: Authentication, JWT issuance, account provisioning, and club member onboarding request flow
type: feature
version: "1.0"
last_updated: "2026-06-11"
criticality: CRITICAL
metadata:
  owner: backend
  affected_services: [karate-tournament-backend, karate-ops-fe]
  affected_domains: [auth, clubs]
source_references:
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/web/AuthController.java, line_range: "1-end", note: "Login, register, /me endpoints" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/service/impl/AuthServiceImpl.java, line_range: "1-end", note: "Login logic, seed admin, JWT assembly" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/service/impl/AccountRequestServiceImpl.java, line_range: "1-end", note: "Account request create + decide" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/service/impl/AccountProvisioningServiceImpl.java, line_range: "1-end", note: "Person + AppUser + OrganizationMember creation" }
  - { file_path: karate-ops-fe/src/App.tsx, line_range: "1-end", note: "LoginPage, RegisterPage routes" }
knowledge_graph_refs:
  - { community: "auth-security", hub_node: "AppUser" }
related_context_files:
  - context/services/auth-module.md
  - context/data-models/auth-domain-entities.md
  - context/client-rules/client-specific-rules.md
---

# Feature: Authentication & Account Onboarding

## Actors
- **Unauthenticated visitor** — can log in or submit a club membership request
- **Club Manager (CLUB_MANAGER)** — approves or rejects membership requests
- **Global Admin (GLOBAL_ADMIN)** — can do everything; seeded on startup

## Flow 1: Login

**Trigger**: FE submits `POST /api/auth/login`

**Steps**:
1. `AuthController.login()` receives `LoginRequest { email, password }`
2. `AuthServiceImpl.login()`:
   a. Normalize credential: `email.trim().toLowerCase()`
   b. `users.findByEmailOrUsername(normalized)` — searches both email and username columns
   c. If not found → `UnauthorizedException("Invalid email or password")` (no hint about which field)
   d. If `user.status != "ACTIVE"` → `UnauthorizedException`
   e. If `user.passwordHash == null` → `UnauthorizedException`
   f. `BCryptPasswordEncoder.matches(rawPassword, hash)` → if false → `UnauthorizedException`
   g. `user.lastLoginAt = Instant.now()`
   h. Build `AuthenticatedPrincipal(userId, primaryOrganizationId, email, displayName, roles)`
   i. `roles = roleAssignments.findByUser_Id()` → `SystemRole.valueOf(assignment.role.code)` + always add `MEMBER`
   j. `jwtService.createToken(principal)` → HMAC-SHA256 JWT, expiry = `app.security.jwt.expires-minutes * 60`
3. Returns `AuthResponse { accessToken, "Bearer", expiresInSeconds, AuthUserResponse }`

**Business Rules**:
- Same error message for wrong email and wrong password (prevents user enumeration)
- `MEMBER` is always added to roles regardless of DB assignments
- `primaryOrganizationId` in JWT comes from `AppUser.primaryOrganization` (nullable)

**Outcome**: FE stores `accessToken` in `sessionStorage["karate-ops.authToken"]`

## Flow 2: Club Member Onboarding (Account Request)

**Trigger**: New member wants to join a club. FE at `/register`.

**Step A — Lookup club (public, no auth)**:
- `GET /api/public/clubs/lookup?code=ABC123`
- Finds `Organization` by code (case-insensitive), must be `type=CLUB` and `status=ACTIVE`
- Returns `PublicClubLookupResponse { id, name, shortName, code }`

**Step B — Submit request (public, no auth)**:
- `POST /api/account-requests` — `AccountRequestCreateRequest { organizationCode, displayName, email, phone, gender, birthDate, currentAddress }`
- Validations:
  - Email normalized to lowercase, must be unique in `users` table (not already registered)
  - No existing PENDING request for same `(email, organization)`
- Creates `AccountRequest { status=PENDING }`
- Returns `AccountRequestResponse`

**Step C — Club Manager reviews**:
- `GET /api/organizations/{id}/account-requests?status=PENDING` — requires `ROSTER_MANAGE` permission
- `PATCH /api/organizations/{id}/account-requests/{requestId}/decision` — `{ status: APPROVED|REJECTED, decisionNote }`
- **If REJECTED**: `decisionNote` required; calls `AccountNotificationService.accountRejected()`
- **If APPROVED**:
  1. `AccountProvisioningService.createMemberAccount()`:
     a. Creates `Person` (displayName, email, phone, gender, birthDate, address)
     b. Creates `AppUser` (email, username via `UsernameGenerator`, status=ACTIVE, passwordHash=BCrypt("123456"))
     c. Creates `OrganizationMember` (role=ATHLETE, status=ACTIVE, joinedAt=today)
  2. `accountRequest.approvedUser = provisioned.user`
  3. `AccountNotificationService.accountApproved(request, username, "123456")`
  4. Returns `MemberAccountCreateResponse { member, username, temporaryPassword="123456" }`

**Business Rules**:
- Reject requires `decisionNote` — throws `BadRequestException` if blank on REJECTED
- APPROVED transitions: can only decide once — `BusinessConflictException("Account request was already decided")` if status != PENDING
- Temporary password is hardcoded `"123456"` (see known-issues/patterns.md Pattern 7)

## Flow 3: /me (Current User Profile)

**Trigger**: FE on load calls `GET /api/auth/me`

**Logic**: `AuthServiceImpl.me(actor)` — loads `AppUser` by `actor.userId()`, returns `AuthUserResponse` with roles from JWT actor.

## Edge Cases

| Scenario | Behavior |
|----------|----------|
| Email already registered but requesting again | `BusinessConflictException("Email is already registered")` at `POST /account-requests` |
| Pending request already exists for same email+org | `BusinessConflictException("This email already has a pending request for this club")` |
| Club code not found or not CLUB type | `ResourceNotFoundException` |
| Password < 8 chars (seed validation) | `BadRequestException("Password must have at least 8 characters")` |
| `register-club-manager` endpoint | Always throws `BadRequestException("Club manager registration is temporarily disabled")` |

## Related APIs
- `POST /api/organizations/{id}/member-accounts` — direct provisioning (skip request flow, CLUB_MANAGER only)
- `POST /api/organizations/{id}/users/{userId}/club-manager-role` — promote existing user to CLUB_MANAGER

## Regional Variants
None. Login and onboarding flow is identical across all deployments.
