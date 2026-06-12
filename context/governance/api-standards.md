---
name: api-standards
description: REST API conventions actually used — URI style, response envelope, validation, auth propagation, error patterns
type: rule
version: "1.0"
last_updated: "2026-06-11"
criticality: HIGH
metadata:
  owner: tech-lead
  affected_services: [karate-tournament-backend, karate-ops-fe]
  affected_domains: [all]
source_references:
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/web/ApiResponse.java, line_range: "1-end", note: "Canonical envelope record" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/web/ApiResponseBodyAdvice.java, line_range: "1-end", note: "Auto-wrapping advice" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/web/ApiExceptionHandler.java, line_range: "1-end", note: "Global exception → HTTP mapping" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/web/TournamentController.java, line_range: "1-end", note: "Reference controller" }
  - { file_path: karate-tournament-backend/src/main/java/com/karate/tournament/web/MatchController.java, line_range: "1-end", note: "Event-sourced endpoint pattern" }
knowledge_graph_refs:
  - { community: "api-layer", hub_node: "ApiResponseBodyAdvice" }
related_context_files:
  - context/governance/coding-standards.md
  - context/architecture/backend.md
---

# API Standards

## Response Envelope

Every REST response (success and error) is wrapped in `ApiResponse<T>`:

```java
public record ApiResponse<T>(
    boolean success,           // true = ok, false = error
    int status,                // HTTP status code
    String code,               // machine-readable code: "SUCCESS" or error name
    String message,            // human-readable description
    T data,                    // payload (null for errors)
    Instant at,                // UTC response timestamp
    String path,               // request URI
    List<ApiErrorDetail> details  // validation field errors; empty list on success
) { }

public record ApiErrorDetail(String field, String message) { }
```

**Wrapping rules** (enforced by `ApiResponseBodyAdvice`):
- If response body is already `ApiResponse<?>` → not re-wrapped
- If HTTP status is 204 NO_CONTENT → null body, no wrapping
- All other return values → wrapped automatically
- Applies to all controllers in `com.karate.tournament.web` package

**Frontend unwrapping** (`apiClient.ts`):
- Checks `response.success == true` → returns `response.data`
- On 401 → clears `sessionStorage["karate-ops.authToken"]` → redirects to `/login`
- On other errors → throws with `response.message`

### Success Response Examples

```json
// POST /api/tournaments → 201 Created
{
  "success": true,
  "status": 201,
  "code": "SUCCESS",
  "message": "Created",
  "data": { "id": "...", "name": "Karate Open 2024", ... },
  "at": "2026-06-11T10:30:45.123Z",
  "path": "/api/tournaments",
  "details": []
}

// DELETE /api/tournaments/{id} → 204 No Content
(empty body)
```

### Error Response Examples

```json
// 400 Validation failed
{
  "success": false,
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "name must not be blank",
  "data": null,
  "at": "2026-06-11T10:30:45.123Z",
  "path": "/api/tournaments",
  "details": [{ "field": "name", "message": "must not be blank" }]
}

// 403 Forbidden
{
  "success": false,
  "status": 403,
  "code": "FORBIDDEN",
  "message": "Tournament management permission is required",
  "data": null, ...
}

// 404 Not Found
{
  "success": false,
  "status": 404,
  "code": "RESOURCE_NOT_FOUND",
  "message": "Tournament not found: 550e8400-...",
  "data": null, ...
}

// 409 Business Conflict
{
  "success": false,
  "status": 409,
  "code": "BUSINESS_CONFLICT",
  "message": "Email is already registered",
  "data": null, ...
}
```

---

## URI Style

**Pattern**: resource-centric, plural nouns, hierarchical by ownership scope.

```
/api/{root-resource}                        # top-level resource
/api/{root-resource}/{id}                   # single item
/api/{root-resource}/{id}/{sub-resource}    # nested collection
/api/{root-resource}/{id}/{sub-resource}/{subId}
/api/{root-resource}/{id}/{sub-resource}/{subId}/{action}  # action verb only on verbs

# Examples:
GET    /api/tournaments
POST   /api/tournaments
GET    /api/tournaments/{id}
PATCH  /api/tournaments/{id}
DELETE /api/tournaments/{id}
GET    /api/tournaments/{id}/participants
POST   /api/tournaments/{id}/participants
PATCH  /api/tournaments/{id}/participants/{pId}/status   # "status" is a sub-resource here
POST   /api/categories/{id}/draw                         # "draw" is an action
POST   /api/matches/{id}/events                          # "events" is an event collection
POST   /api/matches/{id}/result                          # "result" is a sub-resource
```

**Inconsistency to watch**: some controllers use `/api/{resource}` at top level (AuthController, MatchController, PersonController), while club-related sub-resources nest under `/api/organizations/{id}/...`. Mix is intentional — org-scoped resources must be nested for permission scoping.

**No action verbs in path** except: `/draw`, `/ensure-today`, `/day-off`, `/assign-match`, `/apply` (fee item apply), `/decision` (account request), `/result` (match result). These are intentional CQRS-style commands.

---

## HTTP Methods & Status Codes

| Operation | Method | Success Status | Notes |
|-----------|--------|----------------|-------|
| List collection | GET | 200 | Returns array |
| Get single | GET | 200 | 404 if not found |
| Create | POST | 201 CREATED | @ResponseStatus(HttpStatus.CREATED) |
| Partial update | PATCH | 200 | All fields nullable; null = no change |
| Full update | PUT | 200 | Not used in this codebase |
| Delete (soft) | DELETE | 204 NO_CONTENT | Physical delete never used |
| Custom action | POST | 200 or 201 | Depends on whether new resource created |

---

## Request DTO Conventions

All request DTOs are **Java records** (immutable). Validation annotations from Jakarta Bean Validation:

```java
public record TournamentCreateRequest(
    @NotBlank String name,          // required string
    @NotNull UUID orgId,            // required object
    @Min(1) @Max(99) Integer count, // range constraint
    String description,             // optional — send null if absent
    LocalDate startsOn              // optional date
) { }
```

**Rules:**
- Required fields: `@NotBlank` (String) or `@NotNull` (objects)
- Optional fields: no annotation, nullable in JSON
- Enum fields: sent as string name (e.g. `"KUMITE"`, `"MONTHLY"`)
- UUID fields: sent as string `"550e8400-e29b-41d4-a716-446655440000"`
- Dates: ISO-8601 `"2026-06-11"` for LocalDate
- Timestamps: ISO-8601 with Z suffix for Instant
- No custom validators exist — all via standard annotations

**Update DTOs** (PATCH): same record as create, all fields nullable. Service layer does null-check per field:
```java
if (request.name() != null) entity.name = request.name();
```

---

## Response DTO Conventions

All response DTOs are **Java records** (immutable).
- Always include `id` (UUID)
- Denormalized name fields included for UI convenience: `ownerOrganizationName`, `athleteName`, `participantName`
- No nested response objects beyond 1 level deep (no deep nesting)
- Enums returned as string names
- Timestamps as Instant (ISO-8601 UTC)
- Dates as LocalDate (ISO-8601)

---

## Authentication Propagation

```
Request: Authorization: Bearer <jwt-token>
→ JwtAuthenticationFilter extracts and verifies
→ Sets SecurityContextHolder with UsernamePasswordAuthenticationToken(principal, null, authorities)
→ ROLE_* prefix added to each SystemRole for Spring authority names

Service layer: inject CurrentActorProvider, call currentActor() to get userId + roles
PermissionService: wraps CurrentActorProvider, provides require* methods
DO NOT: call SecurityContextHolder.getContext() directly in services — use CurrentActorProvider
```

---

## Validation Pattern

Bean validation triggered by `@Valid` on controller `@RequestBody`:

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public TournamentResponse create(@Valid @RequestBody TournamentCreateRequest request) { ... }
```

On failure: `MethodArgumentNotValidException` → `ApiExceptionHandler` → 400 with `code: VALIDATION_FAILED` and `details: [{ field, message }, ...]`.

---

## Exception → HTTP Mapping

| Exception | code | HTTP |
|-----------|------|------|
| `ResourceNotFoundException` | `RESOURCE_NOT_FOUND` | 404 |
| `BadRequestException` | `BAD_REQUEST` | 400 |
| `BusinessConflictException` | `BUSINESS_CONFLICT` | 409 |
| `ForbiddenException` | `FORBIDDEN` | 403 |
| `UnauthorizedException` | `UNAUTHORIZED` | 401 |
| `MethodArgumentNotValidException` | `VALIDATION_FAILED` | 400 |
| `IllegalArgumentException` | `BAD_REQUEST` | 400 |
| `ConstraintViolationException` | `BAD_REQUEST` | 400 |

Usage rule: throw the most specific subclass from services. Never throw `RuntimeException` directly.

---

## File Upload / Download

No file upload endpoints currently. CSV exports via:
```
GET /api/tournaments/{id}/exports/entries.csv
GET /api/tournaments/{id}/exports/schedule.csv
GET /api/tournaments/{id}/exports/medals.csv
```
Return `text/csv` content type. Not wrapped by `ApiResponseBodyAdvice` (CSV response bypasses JSON wrapping).

---

## WebSocket API (STOMP)

**Endpoint**: `ws://{host}/ws` (STOMP protocol)

**Subscribe topics** (server → client broadcast):
```
/topic/tatamis/{tatamiId}               → MatchResponse (full match state on any event)
/topic/tournaments/{tournamentId}/dashboard → DashboardOverviewResponse (on any match change)
```

**Publish after mutations**: `RealtimePublisher.publishMatch(MatchResponse)` called by:
- `MatchServiceImpl.recordEvent()` — every score event
- `MatchServiceImpl.confirmResult()` — result confirmation
- `TatamiServiceImpl.assignMatch()` — tatami assignment

**No client → server STOMP messages** in current implementation. All client actions go via REST.

---

## Deprecation Pattern

No deprecated endpoints currently. Pattern when deprecating: add `@Deprecated` annotation to controller method + return `X-Deprecated: true` header + document in known-issues. No automated deprecation tooling.

---

## Common Inconsistencies to Watch

1. **Mixed base paths**: some controllers use `@RequestMapping("/api/…")` on class, others set it per method. Check `MatchController` — uses `@RequestMapping("/api")` at class level.
2. **PATCH vs PUT**: codebase uses PATCH everywhere for updates. Never add a PUT endpoint — it would be out of pattern.
3. **Enum parsing in query params**: some endpoints manually parse enum query params (e.g. `AccountRequestController.parseStatus()`). If invalid enum string → `BadRequestException`, not 500.
4. **No pagination**: all list endpoints return full unbounded lists. Be aware for large datasets (AttendanceRecord, MatchScoreEvent).
5. **register-club-manager endpoint**: exists but throws `BadRequestException("Club manager registration is temporarily disabled")` — do not rely on it.
