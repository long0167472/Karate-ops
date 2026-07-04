---
name: create-harness
description: Bootstrap a verification harness (invariant tests, static guards, CI gates, meta-rules) for a project. Use when the user asks to create, scaffold, or strengthen a harness / guardrails / verification layer for a repo or a new project. The skill drives an interview-first, red-first, human-approved process — it produces a DRAFT harness and stops for approval; it never merges its own rules.
---

# Create Harness

You are building the **enforcement layer** that will later be trusted to judge AI-written code.
Because the harness is the judge, it is the ONE artifact you must NOT fully self-certify.
Your job: do 90% of the mechanical work, and structure the remaining 10% (which rules are true)
as explicit human decisions.

## Non-negotiable laws (read before doing anything)

1. **Invariants come from truth, not from code.** You may read code to learn *how* to write
   tests (frameworks, naming, wiring), but the *rules themselves* must come from the human,
   requirements docs, domain model, or ADRs. Any rule you inferred yourself MUST be tagged
   `[AI-INFERRED — needs confirmation]` in every artifact that mentions it.
2. **Red-first, always.** A test that has never failed proves nothing. For every invariant
   test you write, you must deliberately break the rule (temporary sabotage on a scratch
   change, reverted afterwards) and capture evidence that the test went red. No red evidence
   → the test does not ship.
3. **Propose, never ratify.** The final output is a DRAFT on a branch plus an approval
   checklist. You do not merge, you do not mark the harness "done", you do not weaken or
   delete an existing harness rule. If an existing rule blocks you, report it — that is the
   harness working.
4. **Role separation.** If you were also asked to write feature code in this session, keep
   harness changes in separate commits and call this out in the summary — the reviewer must
   be able to approve rules independently of code.
5. **Risk-first ordering.** Encode the "catastrophic if wrong" rules first (money, authz,
   data integrity, irreversible operations), happy paths last. Never fill quota with easy tests.
6. **No mirror tests.** A test must assert *intent* (the spec), never re-derive the expected
   value by calling the same logic it is testing. If you cannot state the rule without
   reading the implementation, you don't have a rule — you have a snapshot. Flag it instead
   of writing it. See `references/anti-patterns.md` for the full rejection list.

## Process (5 phases, in order)

### Phase 1 — Interview (do NOT skip, do NOT guess)

Ask the human the elicitation questions in `references/interview-questions.md` using
AskUserQuestion (batch them; 2–4 questions per call). Minimum you must obtain:

- What failures are catastrophic? (money, security, data loss, legal)
- Which rules must never be violated regardless of refactors?
- What are the architectural boundaries / ownership rules?
- What bugs have already happened (or are feared) that a gate should have caught?

Also mine existing sources yourself before asking, so questions are informed, not lazy:
`CLAUDE.md` / `AGENTS.md`, `context/` or docs dirs, ADRs, known-issues files, CI configs,
existing tests. Everything you mine is still `[AI-INFERRED]` until the human confirms it.

### Phase 2 — Rule ledger

Write `.harness/invariants.md` (template: `references/templates/invariants.md.tpl`).
Every rule gets: ID, statement, WHY (consequence of violation), source tag
(`[HUMAN]` / `[DOC:path]` / `[AI-INFERRED]`), enforcement tier, and (later) a link to the
enforcing test/guard + red-run evidence.

Assign each rule the **cheapest tier that can catch it**:

| Tier | Mechanism | Cost | Use for |
|------|-----------|------|---------|
| 1 | static guard (grep/AST/diff script) | seconds | forbidden strings, protected files, structural bans |
| 2 | compile / typecheck | seconds | shape errors |
| 3 | invariant / contract / authz test | minutes | behavioral rules |
| 4 | human review (CODEOWNERS) | human | changes to the harness itself |

Also write `.harness/rules.yaml` (machine-readable map rule → enforcer → severity).

### Phase 3 — Build enforcers

- Tier-1 guards → `scripts/harness/*.sh` (template: `references/templates/static-guard.sh.tpl`).
  Each script: exit 0 = pass, exit 1 = fail with a message naming the rule ID.
- Tier-3 tests → the project's native test tree, under a `harness/` package/dir so they are
  identifiable and lockable. Use the project's existing test framework and conventions.
- Wire everything into CI (template: `references/templates/harness.yml.tpl`) as fail-fast
  gates: static → compile → tests → coverage. Local hooks and CI must run the SAME scripts
  (CI-mirror rule): never implement a check twice.

### Phase 4 — Red-first proof

For each guard/test: sabotage → run → capture the failing output → revert sabotage → run
again green. Record a one-line evidence entry (what was broken, what went red) per rule in
`.harness/invariants.md`. A rule without red evidence must be listed under "UNPROVEN" in the
final summary.

### Phase 5 — Meta-rules + handoff

Install the anti-cheating layer:

- `CODEOWNERS` entry locking `.harness/`, `scripts/harness/`, the harness test dirs, and the
  CI workflow to human reviewers (template: `references/templates/CODEOWNERS.tpl`).
- CI checks that `--no-verify` / skip-ci markers are not used and that harness paths were not
  modified without the required review (branch protection note in the summary — you cannot
  set branch protection yourself; tell the human to).
- Coverage gate config if the project has coverage tooling; never lower an existing threshold.

Then STOP and hand off. Final summary must contain:

1. Table of rules: ID · statement · source tag · tier · red evidence (or UNPROVEN).
2. Explicit list of every `[AI-INFERRED]` rule awaiting confirmation.
3. What the human must do: review checklist, enable branch protection, confirm CODEOWNERS.
4. What you deliberately did NOT do (merge, self-approve, weaken existing rules).

## Scope discipline

- New/greenfield project: it is normal that Phase 3 produces only a walking-skeleton gate
  plus 2–3 highest-risk invariants. Do not fabricate tests for code that doesn't exist.
- Existing project: do not "fix" failing code to make your new tests pass in the same run —
  a new invariant test that fails against current code is a FINDING; report it, don't silently
  patch production code or water down the test.
- Never edit anything under an existing `.harness/` or `scripts/harness/` except to ADD;
  modifications require the human path.
