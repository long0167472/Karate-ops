# Recon Playbook — mining an unfamiliar codebase for candidate invariants

Use when onboarding a codebase you know nothing about. Recon produces HYPOTHESES with
evidence, never truth: everything found here is `[AI-INFERRED]` until a human confirms it.
Recon upgrades the Phase 1 interview from open questions to evidence-backed confirmation
("I found X in commit Y — is this a hard rule?"). It never replaces the interview.

Time-box the whole pass; depth-first on the risk surfaces, not breadth-first on everything.

## Vein 1 — Money & authority surface (always first)

Grep for the vocabulary of catastrophe; every hit area is a candidate tier-3 invariant:

- money: `price|amount|fee|billing|invoice|payment|discount|refund|total`
- authz: `role|permission|grant|isAdmin|hasAccess|scope|tenant|organizationId`
- destruction: `delete|remove|drop|truncate|purge` (physical vs soft-delete?)
- irreversibility: `publish|send|charge|transfer|notify` (side effects with no undo)

For each cluster: who computes the value, who is trusted, what happens on bypass.

## Vein 2 — Git history = the scar record (highest-value vein)

Past bugs are PROVEN failure modes — the strongest invariant candidates that exist:

```bash
git log --oneline --grep='fix\|bug\|hotfix\|revert' -i --since='2 years ago' | head -50
git show <hash> --stat        # what did the fix touch?
```

Each recurring fix theme → one candidate regression invariant, cite the commit hash as
evidence. A bug fixed twice is a rule screaming to be enforced.

## Vein 3 — Schema & migrations = half-encoded invariants

DB constraints (NOT NULL, UNIQUE, FK, CHECK), migration files, and any "do not edit"
markers. A DB constraint is an invariant someone already paid for — check whether the
application layer respects or duplicates it, and whether migrations are append-only.

## Vein 4 — Existing tests = the map of past fear

- What IS tested → what the previous team was afraid of (candidate confirmations).
- What is NOT tested but sits on a Vein-1 surface → the gap list, your priority.
- Test names/comments often state rules verbatim ("should never charge twice").

## Vein 5 — Hotspots = where risk concentrates

```bash
git log --format= --name-only --since='1 year ago' | sort | uniq -c | sort -rn | head -20
```

Most-churned files × Vein-1 surfaces = where the first invariant tests belong.

## Also read (cheap, do first): README, CLAUDE.md/AGENTS.md, docs/, ADRs, context/ dirs,
CI configs, known-issues files. Treat as `[DOC:path]` sourced — stronger than inference,
still weaker than `[HUMAN]`.

## Output of recon

A hypothesis table feeding Phase 1 and Phase 2:

| Candidate rule | Evidence (file/commit) | Risk if wrong | Proposed tier | Status |
|----------------|------------------------|---------------|---------------|--------|

Rules for the table:
- Every row cites concrete evidence (path or commit hash) — no vibes.
- Current code may be WRONG: inferring a rule from buggy behavior freezes the bug
  (anti-patterns.md #3). When behavior looks suspicious, phrase the hypothesis as a
  question, not a rule.
- Cap the list (~10–15 highest-risk candidates). A 60-row hypothesis dump makes the
  human rubber-stamp instead of think — which defeats the approval step.
