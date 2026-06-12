---
name: frontend-architecture
description: React SPA architecture — manual routing, API client, tatami real-time views, role system
type: architecture
last_updated: 2026-06-12
criticality: high
metadata:
  owner: frontend
  affected_services: [karate-ops-fe]
  affected_domains: [auth, tournaments, clubs, tatami]
source_references:
  - symbol: App
    file: karate-ops-fe/src/App.tsx
  - symbol: apiGet, apiPost, apiPatch, apiPut, apiDelete
    file: karate-ops-fe/src/apiClient.ts
  - symbol: useScoreboard
    file: karate-ops-fe/src/useScoreboard.ts
  - symbol: useManualTatami
    file: karate-ops-fe/src/useManualTatami.ts
  - symbol: Homepage
    file: karate-ops-fe/src/components/Homepage.tsx
---

# Frontend Architecture

## Routing

Manual — no React Router. `App.tsx` reads `window.location.pathname` and renders the matching component.  
All top-level pages live in `App.tsx` (one large file) except `ClubManagementPage` (separate import).

| Path | Component |
|------|-----------|
| `/` | `PublicHomePage` |
| `/login` | `LoginPage` |
| `/register` | `RegisterPage` |
| `/app` | **role-based** (see below) |
| `/member` | `MemberPortalPage` |
| `/clubs[/:id]` | `ClubManagementPage` |
| `/tournaments` | `TournamentManagementPage` |
| `/dashboard/tournaments/:id` | `TournamentDashboardPage` |
| `/control?tatamiId=&tournamentId=` | `ControlPage` |
| `/display?tatamiId=&tournamentId=` | `DisplayPage` |
| `/judge?tatamiId=&tournamentId=` | `JudgePage` |
| `/overlay?...` | `OverlayPage` |

### `/app` landing is role-based

`App.tsx` branches on `canManageClub(effectiveUser)` (= `GLOBAL_ADMIN | CLUB_MANAGER | COACH`):

- **Can manage a club** → `ClubManagementPage` with no `clubId` → renders the **club directory** (global admin sees all clubs via `fetchClubDirectory(isAdmin, primaryOrganizationId)`; managers/coaches see their own). Each card links to `/clubs/:id` (the management workspace).
- **Regular member** → `HomePage` wrapper → `components/Homepage.tsx`, a member dashboard with two tabs:
  - **"Clb của tôi"** — fee summary (`/api/me/fees`), training calendar + per-session leave requests (`/api/me/attendance`, `POST /api/me/attendance/leave-requests`), notifications (`useNotifications` → `/api/notifications`).
  - **"Xem các giải đấu"** — upcoming tournaments (`/api/public/tournaments?phase=UPCOMING`).

  Styling reuses the `club-*` warm-theme classes from `styles.css` (same visual language as `ClubManagementPage`). Tab switching uses a single keyed `motion.div` (NOT `AnimatePresence mode="wait"`, which deadlocked).

`MemberPortalPage` (`/member`) is the older member view and still reachable directly; it is no longer the `/app` landing.

## API client (`apiClient.ts`)

All HTTP calls go through `apiGet/apiPost/apiPatch/apiPut/apiDelete`.  
JWT stored in `sessionStorage["karate-ops.authToken"]`. 401 → clear token + redirect to `/login`.  
Backend wraps all responses in `ApiEnvelope<T>`; client auto-unwraps `.data`.

`VITE_API_BASE_URL` sets origin; defaults to same-origin (dev proxies `/api`, `/ws` to `:8080`).

## Tatami views (`/control`, `/display`, `/judge`, `/overlay`)

Shared live state via `useScoreboard`:
1. Initial REST fetch of current match
2. Subscribe to STOMP `/topic/tatamis/{tatamiId}` for live updates

Actions POST to `/api/matches/{id}/events` and `/api/matches/{id}/result`.

If no `tatamiId` param → `useManualTatami` (local-only state) — free tatami demo mode, no auth required.

## Club features

Split into `src/features/clubs/`:
- `clubApi.ts`, `clubUtils.ts`, `clubConstants.ts`
- Tab components: `MembersTab`, `AttendanceTab`, `FeesTab`, `LeaveRequestsTab`

Everything else lives directly in `src/`.

## Role system

`AuthUserResponse.roles` string array: `GLOBAL_ADMIN` | `CLUB_MANAGER` | `COACH` | `MEMBER`.  
`GLOBAL_ADMIN` has a "View as" selector (persisted in `localStorage["karate-ops.viewAsRole"]`, values `ACTUAL|GLOBAL_ADMIN|CLUB_MANAGER|MEMBER`) to emulate other roles — `effectiveRoleUser()` rewrites the role array, which then drives the role-based `/app` landing. Note: the selector currently lives on `MemberPortalPage`/`OperationsHub`, not on the club-directory landing, so an admin who lands on the club list has no in-page toggle to preview the member `Homepage`.

## Animations

- Public landing page: GSAP + `@gsap/react`
- Page transitions and auth forms: Framer Motion
