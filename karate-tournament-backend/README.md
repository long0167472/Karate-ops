# Karate Tournament Backend

Spring Boot backend for tournament management, tatami scoring, realtime updates, and dashboards.

## Stack

- Java 17 target, Spring Boot 3.5, Maven
- PostgreSQL 16/17
- Flyway migrations
- Spring Data JPA
- WebSocket/STOMP topics for tatami and dashboard updates

## Run

Use JDK 17 or newer. On this machine, Android Studio includes a JBR 21 runtime:

```powershell
$env:JAVA_HOME='C:\Users\hoang\AppData\Local\Programs\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

Start PostgreSQL:

```powershell
docker compose up -d postgres
```

The local default DB config is:

```text
DB_URL=jdbc:postgresql://localhost:5432/karate_tournament?options=-c%20TimeZone=UTC
DB_USERNAME=postgres
DB_PASSWORD=123456
```

Run the app:

```powershell
mvn spring-boot:run
```

Default API URL: `http://localhost:8080`

## Auth model

V1 has no login. Every request is treated as `GLOBAL_ADMIN` through `CurrentActorProvider`, but permission checks are already placed in services so a future Spring Security/JWT provider can replace the global actor without changing the API surface.

## Club APIs

- `GET/POST /api/organizations/{organizationId}/members`
- `PATCH/DELETE /api/organizations/{organizationId}/members/{memberId}`
- `GET/POST /api/organizations/{organizationId}/roster`
- `PATCH/DELETE /api/organizations/{organizationId}/roster/{rosterId}`
- `GET/POST /api/organizations/{organizationId}/attendance-sessions`
- `GET/PATCH /api/attendance-sessions/{sessionId}`
- `POST /api/attendance-sessions/{sessionId}/records`
- `PATCH /api/attendance-sessions/{sessionId}/records/{recordId}`
- `GET /api/dashboard/organizations/{organizationId}/overview`
- `GET /api/dashboard/organizations/{organizationId}/attendance`
- `GET /api/dashboard/organizations/{organizationId}/athletes/{athleteId}`

Club roster is intentionally not a loose CRUD list: an athlete must be backed by an active organization member before joining a club roster, and tournament entries validate the athlete against the delegation organization.

## Realtime topics

- `/topic/tatamis/{tatamiId}`
- `/topic/tournaments/{tournamentId}/dashboard`
