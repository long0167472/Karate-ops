# Karate Ops FE

Frontend for Karate Ops: club-to-tournament tatami operations connected directly to the Spring Boot backend.

## Stack

- React + Vite + TypeScript
- Framer Motion for scoreboard transitions
- REST + STOMP WebSocket against `karate-tournament-backend`
- Express static server for production builds

## Run Locally

Start the Spring backend first on `http://localhost:8080`.

```powershell
npm install
npm run dev
```

Default dev URL: `http://localhost:5173`

Vite proxies:

- `/api` -> `http://localhost:8080`
- `/ws` -> `ws://localhost:8080`

Override backend URL when needed:

```powershell
$env:VITE_API_BASE_URL='http://localhost:8080'
npm run dev
```

## Pages

- `/` - public Karate Ops landing page, no login required
- `/app` - authenticated operations hub with tournament and tatami selector
- `/control?tournamentId=...&tatamiId=...` - tatami operator console
- `/display?tournamentId=...&tatamiId=...` - fullscreen scoreboard
- `/judge?tournamentId=...&tatamiId=...&judge=1` - Kata judge voting
- `/overlay?tournamentId=...&tatamiId=...` - OBS overlay
- `/clubs` and `/clubs/:id` - authenticated club workspace and unified club dashboard
- `/health` - static server health in production mode

## V1 Notes

- Data is sourced from Spring Boot REST endpoints.
- Realtime match updates come from STOMP topic `/topic/tatamis/{tatamiId}`.
- Undo, VR state, Kata reveal phase, and direct match metadata editing are disabled until backend state supports them.
