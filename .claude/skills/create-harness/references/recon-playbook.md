# Recon Playbook — mining an unfamiliar codebase for candidate invariants

Use when onboarding a codebase you know nothing about. Recon produces HYPOTHESES with
evidence, never truth: findings are tagged `[DOC:path]` (curated docs, staleness-checked)
or `[AI-INFERRED]` (mined from code/history) — see the trust hierarchy below for what each
tag may ship at without a human. Recon upgrades the Phase 1 interview from open questions
to evidence-backed confirmation ("I found X in commit Y — is this a hard rule?"). It never
replaces the interview.

Time-box the whole pass; depth-first on the risk surfaces, not breadth-first on everything.

## Vein 0 — The curated context base (best source; mine FIRST)

If the project practices context/compound engineering (`context/` dirs, `CLAUDE.md`/
`AGENTS.md`, ADRs, known-issues files, wiki indexes), start there: these are **distilled
human answers written down while memory was fresh** — higher quality than a live interview.
Known-issues/post-mortem files are the richest single source: each documented incident is a
proven failure mode with its rule already articulated.

Two cautions before trusting a context file:

- **Staleness check** — context drifts from code. Verify:
  `git log --oneline --since='<context file last_updated>' -- <source files it describes>`
  If sources changed after the doc, mark the hypothesis `[DOC:path — possibly stale]`.
- **Advisory phrasing** — context is written for human understanding, not as testable
  statements. Rewrite each rule into a falsifiable form before it enters the ledger; if it
  can't be made falsifiable, it stays documentation, not an enforcer.

A rich Vein 0 shrinks the interview: ask only what the context base does NOT answer, plus
the promotion decision (see SKILL.md severity policy).

## Vein 1 — Money & authority surface (always first among code veins)

Grep for the vocabulary of catastrophe; every hit area is a candidate tier-3 invariant:

- money: `price|amount|fee|billing|invoice|payment|discount|refund|total`
- authz: `role|permission|grant|isAdmin|hasAccess|scope|tenant|organizationId`
- destruction: `delete|remove|drop|truncate|purge` (physical vs soft-delete?)
- irreversibility: `publish|send|charge|transfer|notify` (side effects with no undo)

For each cluster: who computes the value, who is trusted, what happens on bypass.

## Vein 2 — Git history = the scar record (highest-value CODE vein)

Past bugs are PROVEN failure modes — the strongest invariant candidates that exist:

```bash
git log --oneline --grep='fix\|bug\|hotfix\|revert' -i | head -50   # no --since: don't miss old scars
git show <hash> --stat        # what did the fix touch?
```

Each recurring fix theme → one candidate regression invariant, cite the commit hash as
evidence. A bug fixed twice is a rule screaming to be enforced.

**FIRST verify the history is actually there.** Fresh clones (every Claude Code web session
starts as one), shallow clones, and squash-merge repos may show almost no history — the scar
record is truncated, not empty.

```bash
git rev-list --count HEAD    # a handful on a "mature" project ⇒ history is truncated
git rev-parse --is-shallow-repository
```

If truncated: `git fetch --unshallow` when the network allows; otherwise DOWNGRADE this
vein and lean on Vein 0 (context/known-issues) and the issue tracker for the scar record.
Never conclude "this project has no past bugs" from a thin `git log` — that is a clone
artifact, not a fact about the code.

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

## Source trust hierarchy

`[HUMAN]` (live confirmation) > `[DOC:path]` (curated context, staleness-checked) >
`[DOC:path — possibly stale]` > `[AI-INFERRED]` (pattern-mined from code/history).
The tag determines what severity a rule may ship at without human sign-off (SKILL.md).

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
