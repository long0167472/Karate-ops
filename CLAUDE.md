# CLAUDE.md

## Project Overview

KarateOps — internal platform for Vietnamese karate clubs: member management, training attendance, fee collection, tournament execution, and live match scoring.
Mono-repo: `karate-ops-fe/` (React 18 + TypeScript + Vite) · `karate-tournament-backend/` (Spring Boot 3.5 + Java 17 + Maven + PostgreSQL 16).

## Commands

```powershell
# Backend — set JAVA_HOME first
$env:JAVA_HOME = 'C:\Users\hoang\AppData\Local\Programs\Android Studio\jbr'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
cd karate-tournament-backend
docker compose up -d postgres   # DB on :5432  (user: postgres / 123456)
mvn spring-boot:run             # API on :8080  (admin: admin@karate-ops.local / Admin@123456)
mvn test

# Frontend
cd karate-ops-fe
npm install && npm run dev      # SPA on :5173, proxies /api + /ws → :8080
npm run check                   # tsc --noEmit (only verification step)
```

## Wiki Navigation

**Start every task at**: `context/indexes/context-index.md`
It contains: full file directory · feature→service map · service→feature map · quick lookup tables · 3 example queries.

**Two knowledge systems — never mix:**
| Question | Go to |
|---------|-------|
| What calls what? Where is X defined? | `/graphify query "…"` |
| What does X mean? What rule applies? | `context/` files |

Stale check before acting on any context file:
```powershell
git log --oneline --since="2026-06-11" -- <source_file_path>
```

## Graphify

`graphify-out/` holds `graph.json` + `graph.html` + `GRAPH_REPORT.md`.
- **Before** answering architecture/relationship questions → `/graphify query "<question>"`
- **After** adding/removing/modifying source files → `/graphify . --update`
- Never edit `graphify-out/` by hand.

## CE Workflow (8 steps)

1. Read `context/indexes/context-index.md` — find relevant files
2. Run `/graphify query "…"` — map blast radius before touching shared code
3. Read context files for the feature/service area
4. Read source only if context file is stale or silent on your question
5. Implement the change
6. Run `npm run check` (FE) or `mvn test` (BE)
7. Update relevant `context/` files (`last_updated`, content) if behaviour changed
8. Run `/graphify . --update` if source files were added, removed, or renamed

## Critical Rules

1. **BE owns all business logic** — due dates, billing cycles, fee defaults are computed server-side. FE sends `null`; BE fills. Violating this caused the BillingCycle bug (see `context/known-issues/billing-cycle-leak.md`).
2. **Never edit applied Flyway migrations** — add `V{n+1}__…sql` only. V4 contains dev seed; do not run on prod without checking first.
3. **Soft-delete only** — call `entity.softDelete()`. Never issue physical `DELETE`.
4. **Realtime publish is mandatory** — after any match mutation: `realtimePublisher.publishMatch(matchResponse)`. Skipping freezes all connected tatami views.
5. **Domain invariant chain** — `Person → OrganizationMember → Athlete → ClubRoster → Entry`. Cannot skip steps; service layer enforces this.
6. **Org-scoped isolation** — CLUB_MANAGER sees only their org. Pass `organizationId` in every org-facing call; use `permissionService.require…(orgId)` in BE services, not in controllers.

## Quick Reference

| Question | File |
|---------|------|
| How does login / JWT work? | `context/services/auth-module.md` |
| Why is a member's fee wrong? | `context/services/business-fee-service.md` → `resolveAmount()` |
| How does bracket draw work? | `context/features/draw-brackets.md` |
| What breaks if I change MatchServiceImpl? | `context/features/tatami-match-scoring.md` + `context/known-issues/patterns.md` Pattern 2 |
| What entity fields exist for X? | `context/data-models/data-model-overview.md` (enum master list) → domain file |
