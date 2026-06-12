---
name: client-specific-rules
description: KarateOps deployment rules — auth, data isolation, PII, service calling, forbidden patterns
type: rule
version: "1.0"
last_updated: "2026-06-11"
criticality: CRITICAL
metadata:
  owner: tech-lead
  affected_services: [karate-ops-fe, karate-tournament-backend]
  affected_domains: [all]
source_references:
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/auth/PermissionServiceImpl.java, line_range: "1-end", note: "RBAC enforcement" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/config/SecurityConfig.java, line_range: "1-end", note: "Public endpoint list, BCrypt encoder" }
  - { file_path: karate-tournament-backend/src/main/resources/db/migration/V4__mock_club_data.sql, line_range: "1-end", note: "Dev seed data in migration chain — production risk" }
knowledge_graph_refs:
  - { community: "auth-security", hub_node: "PermissionService" }
related_context_files:
  - context/governance/coding-standards.md
  - context/architecture/overview.md
---

# Client-Specific Rules

## Auth & Access Control

### Rule AUTH-1 — Every service method must check permissions via PermissionService
Never use `SecurityContextHolder.getContext()` directly in service implementations. Always inject `PermissionService` or `CurrentActorProvider` and call their methods.

```java
// CORRECT
@Service
public class SomeServiceImpl {
    private final PermissionService permissions;
    public void doSomething(UUID orgId) {
        permissions.requireClubView(orgId);  // throws ForbiddenException if denied
        // ...
    }
}

// WRONG
SecurityContextHolder.getContext().getAuthentication();  // not in service layer
```

### Rule AUTH-2 — Organization scope is enforced by primaryOrganizationId in JWT
A CLUB_MANAGER can only manage their own organization. The JWT token encodes `primaryOrganizationId`. `PermissionServiceImpl.canManageClub(orgId)` returns true only if `actor.primaryOrganizationId().equals(orgId)`.

If a user needs access to multiple organizations simultaneously, the current model does not support it — a separate token or GLOBAL_ADMIN role is required.

### Rule AUTH-3 — Temporary password for provisioned accounts is hardcoded
`AccountProvisioningService.DEFAULT_TEMPORARY_PASSWORD = "123456"`. All accounts created via `account-requests` approval or `POST /member-accounts` get this password. The user must be told to change it. This is a known security debt.

### Rule AUTH-4 — `MEMBER` role is always added
`SystemRole.MEMBER` is added to every user's role set in `AuthServiceImpl.authResponse()` and `JwtServiceImpl.verify()`. This ensures every authenticated user has the base role. Do not rely on its absence.

### Rule AUTH-5 — Admin seed user has fixed UUID
The seed GLOBAL_ADMIN user ID is `00000000-0000-0000-0000-000000000001`. Do not use this UUID as a hardcoded reference in business logic — it is for system bootstrap only. Seed org ID: `00000000-0000-0000-0000-000000000201`.

### Rule AUTH-6 — `register-club-manager` is disabled
`POST /api/auth/register-club-manager` intentionally throws `BadRequestException`. Do not attempt to implement self-registration via this endpoint — it requires a separate business decision and process.

---

## Service-to-Service Calling Rules

### Rule SVC-1 — No inter-service HTTP calls; all logic is in-process
This is a monolith. Services call other services directly via Spring injection. There are no Feign clients, no REST calls between services, no message queues.

### Rule SVC-2 — Domain invariant chain must be respected
The chain `Person → OrganizationMember → Athlete/ClubRoster → TournamentParticipant → Entry` must be followed in order. You cannot create an Entry without a TournamentParticipant, and you cannot create a TournamentParticipant without an Organization being APPROVED.

Attempting to skip steps produces either a DB constraint violation or a `BusinessConflictException`.

### Rule SVC-3 — After any match mutation, call RealtimePublisher
```java
// In MatchServiceImpl — ALWAYS after saving match state
realtimePublisher.publishMatch(mapper.match(match));
```
Omitting this breaks all connected tatami views silently. The check: after confirm result or record event, all `/display`, `/judge`, `/overlay` tabs must update in real time.

### Rule SVC-4 — Soft delete, never physical delete
Call `entity.softDelete()` and save, never `repository.delete(entity)`. All queries filter `deletedAt IS NULL`. Physical delete would bypass these filters and expose orphaned records.

---

## Build & Runtime Environment Rules

### Rule ENV-1 — Java version must be 17+ (Android Studio JBR 21 is available)
```powershell
$env:JAVA_HOME = 'C:\Users\hoang\AppData\Local\Programs\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```
Running with JDK < 17 will fail due to record classes and text blocks used throughout.

### Rule ENV-2 — PostgreSQL must be running before Spring Boot starts
```powershell
docker compose up -d postgres
```
Spring Boot does not start if DB is unavailable. Flyway runs migrations on startup.

### Rule ENV-3 — `V4__mock_club_data.sql` is in the production migration chain
V4 inserts seed/demo data. It runs on any environment that applies Flyway migrations. If running against a production database, this will insert test data. **[NEEDS HUMAN INPUT]: Is V4 intentional for all environments or should it be excluded for prod?**

### Rule ENV-4 — CORS origins configured via environment variable
`APP_CORS_ALLOWED_ORIGINS` — defaults include `:5173` (Vite dev), `:5174`, `:4173` (production build). For production deployment, this must be set to the actual frontend domain.

### Rule ENV-5 — Security can be disabled for development
`app.security.enabled=false` disables all auth checks. Never disable in production. The `GlobalAdminCurrentActorProvider` (`@Profile("test")`) automatically grants GLOBAL_ADMIN access in test profile.

---

## PII and Data Handling Rules

### Rule PII-1 — Person fields are PII
`Person` entity contains: `displayName`, `firstName`, `lastName`, `birthDate`, `gender`, `nationalId`, `email`, `phone`, `currentAddress`, `emergencyContactName`, `emergencyContactPhone`.

Do not log these fields. Do not include them in exception messages. Do not expose `nationalId` in any response unless explicitly required.

### Rule PII-2 — `nationalId` is sensitive
`Person.nationalId` (Vietnamese CCCD/CMND) — do not include in CSV exports, logs, or audit events. [NEEDS HUMAN INPUT]: Is nationalId currently returned in PersonResponse? Verify before export feature work.

### Rule PII-3 — Passwords are BCrypt-hashed, never logged
`AppUser.passwordHash` is BCrypt. Never log the hash or the plaintext password. Never return it in any response.

### Rule PII-4 — Temporary password "123456" is transmitted in responses
`MemberAccountCreateResponse.temporaryPassword` returns the plaintext temporary password. This is sent over HTTPS in the response body and also in `AccountNotificationService` (email/notification). Ensure transport is HTTPS in production. This is a known security debt ([see known-issues/patterns.md]).

---

## Forbidden Patterns

| # | Pattern | Why Forbidden |
|---|---------|---------------|
| 1 | Edit an applied Flyway migration | Corrupts every environment that already applied it |
| 2 | Call `SecurityContextHolder` in service layer | Bypasses the `CurrentActorProvider` abstraction; breaks test profile |
| 3 | Compute due dates or billing cycles in FE | Business logic belongs in BE (BillingCycle rule — see coding-standards.md Rule 1) |
| 4 | Hardcode organization ID or user ID (except seed UUIDs) | Business data; must come from auth context |
| 5 | Skip `requireXxx()` permission call for a new endpoint | Creates authorization bypass silently |
| 6 | `repository.delete(entity)` | Bypasses soft-delete; use `entity.softDelete()` |
| 7 | Share `Organization` data across tenants | Org-scoped isolation is the multi-tenancy boundary |
| 8 | Add logic to FE that duplicates BE domain rules | See BillingCycle leak pattern |
| 9 | Create `@Transactional` on controller methods | Transaction boundary belongs in service layer |
| 10 | Return `null` from a `List`-returning service method | Return empty list; null causes NPE in FE JSON parsing |
