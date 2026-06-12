---
name: backend-architecture
description: Spring Boot 3.5 backend — API envelope, auth, realtime, domain invariant chain, fee model
type: architecture
last_updated: 2026-06-11
criticality: high
metadata:
  owner: backend
  affected_services: [karate-tournament-backend]
  affected_domains: [auth, tournaments, clubs, fees, realtime]
source_references:
  - symbol: ApiResponseBodyAdvice
    file: karate-tournament-backend/src/main/java/com/karate/tournament/advice/ApiResponseBodyAdvice.java
  - symbol: ApiExceptionHandler
    file: karate-tournament-backend/src/main/java/com/karate/tournament/advice/ApiExceptionHandler.java
  - symbol: JwtAuthenticationFilter
    file: karate-tournament-backend/src/main/java/com/karate/tournament/security/JwtAuthenticationFilter.java
  - symbol: RealtimePublisher
    file: karate-tournament-backend/src/main/java/com/karate/tournament/realtime/RealtimePublisher.java
  - symbol: CurrentActorProvider
    file: karate-tournament-backend/src/main/java/com/karate/tournament/security/CurrentActorProvider.java
---

# Backend Architecture

## API envelope

`ApiResponseBodyAdvice` wraps every controller response:

```json
{ "success": true, "status": 200, "code": "...", "message": "...", "data": {...}, "at": "...", "path": "..." }
```

`ApiExceptionHandler` produces the same shape for errors. FE `apiClient.ts` auto-unwraps `.data`.

## Exception hierarchy

```
ApiException (abstract, carries code())
  ├─ ResourceNotFoundException
  ├─ BadRequestException
  ├─ BusinessConflictException
  ├─ ForbiddenException
  └─ UnauthorizedException
```

Throw the appropriate subclass from services; handler maps to HTTP status codes.

## Auth

`JwtAuthenticationFilter` validates `Bearer` tokens → populates `SecurityContextHolder`.  
Services use `CurrentActorProvider` interface (`JwtCurrentActorProvider` impl) — decouples domain logic from Spring Security.

## Realtime (WebSocket / STOMP)

After any match state mutation: call `RealtimePublisher.publishMatch(MatchResponse)`.

- Full match → `/topic/tatamis/{tatamiId}`
- Dashboard snapshot → `/topic/tournaments/{tournamentId}/dashboard`
- WS endpoint: `/ws`

## Service layer

All domain logic behind interfaces in `service/`; implementations in `service/impl/`.  
Repositories extend Spring Data JPA.

## Database migrations

Flyway — `src/main/resources/db/migration/`, naming: `V{n}__{description}.sql`.  
**Never edit applied migrations. Always add a new version.**

## Core domain invariant chain

```
Person → OrganizationMember → Athlete / ClubRoster → TournamentParticipant → Entry
```

`OrganizationMember` must exist before an athlete can join a `ClubRoster`.  
Tournament entries validate athletes against the delegation organization.

## Fee data model (structure only — logic in fee-system.md)

```
ClubFeeRole
  └─ ClubFeeItem (feeKind, dueDay, billingCycle)
       └─ ClubFeeItemRoleAmount (per-role amount)
            └─ OrganizationMemberFeeRole (per-member role assignment)
                 └─ fee assignment record
```

`ClubTrainingScheduleJob` auto-generates attendance sessions from `ClubTrainingSchedule` on scheduled days.

## CORS

Configured via `APP_CORS_ALLOWED_ORIGINS`. Defaults include `:5173`, `:5174`, `:4173`.
