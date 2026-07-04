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

## Process (6 phases, in order)

### Phase 0 — Detect what already exists (idempotency)

Before anything else, check for `.harness/`, `scripts/harness/`, harness CI workflows, and
harness-named test dirs. Two modes:

- **Greenfield** (nothing found): run all phases.
- **Existing harness found**: switch to GAP-ANALYSIS mode. Read the existing ledger, list
  which rules lack enforcers / red evidence / CI wiring, and propose ADDITIONS only. You may
  not modify or delete existing rules, guards, or thresholds (law 3) — if an existing rule
  looks wrong, report it in the summary as a finding for the human.

Also detect: default branch, CI system (GitHub Actions / GitLab / other), test frameworks,
coverage tooling. If the CI system is not GitHub Actions, keep the gate ORDER from the
template and translate the syntax — the structure is the contract, not the YAML dialect.

### Phase 1 — Interview (do NOT skip, do NOT guess)

Ask the human the elicitation questions in `references/interview-questions.md` using
AskUserQuestion (batch them; 2–4 questions per call). Minimum you must obtain:

- What failures are catastrophic? (money, security, data loss, legal)
- Which rules must never be violated regardless of refactors?
- What are the architectural boundaries / ownership rules?
- What bugs have already happened (or are feared) that a gate should have caught?

Before asking, do reconnaissance yourself so questions are evidence-backed, not lazy.
For an unfamiliar/onboarding codebase, run the full `references/recon-playbook.md`
(money/authz surfaces, git bug-fix history, schema constraints, existing tests, hotspots)
and bring its hypothesis table to the interview — ask "I found X in commit Y, is this a
hard rule?" instead of "what are your rules?". For any codebase, at minimum read
`CLAUDE.md`/`AGENTS.md`, `context/` or docs dirs, ADRs, known-issues files, CI configs,
existing tests. Everything you mine is still `[AI-INFERRED]` (or `[DOC:path]`) until the
human confirms it — recon sharpens the interview, it never replaces it.

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

**Severity policy — what may ship without a human.** Human confirmation is an *approval
decision* ("this rule now blocks merges"), not a knowledge test — often the knowledge lives
in the curated context base and the human cannot answer better than the docs. So:

| Source tag | Max severity without human sign-off |
|------------|-------------------------------------|
| `[HUMAN]` | `block` |
| `[DOC:path]` (staleness-checked) | `block` only if the doc states it as a hard rule verbatim; otherwise `warn` |
| `[DOC — possibly stale]` / `[AI-INFERRED]` | `warn` |

`warn`-severity rules run in CI and report, but don't block. Note in the handoff which
`warn` rules are candidates for promotion to `block` after a soak period with no false
positives — promotion is itself a human decision.

### Phase 3 — Build enforcers

- Tier-1 guards → `scripts/harness/*.sh` (template: `references/templates/static-guard.sh.tpl`).
  Each script: exit 0 = pass, exit 1 = fail with a message naming the rule ID.
  **Severity is encoded in the filename**: `block-<rule>.sh` fails the gate; `warn-<rule>.sh`
  is reported but never fails the gate (the CI loop treats the two prefixes differently —
  see harness.yml.tpl). This is how warn-severity rules from the Phase 2 policy actually run.
- Tier-3 tests → the project's native test tree, under a `harness/` package/dir so they are
  identifiable and lockable. Use the project's existing test framework and conventions.
- Wire everything into CI (template: `references/templates/harness.yml.tpl`) as fail-fast
  gates: static → compile → tests → coverage. Local hooks and CI must run the SAME scripts
  (CI-mirror rule): never implement a check twice.
- **Wire the local side of the mirror too** — CI alone means the agent/dev only learns about
  violations after a push. Install BOTH that apply:
  - a git `pre-push` hook (or `pre-commit` for tier-1 only) that loops over
    `scripts/harness/*.sh` — commit it as `scripts/harness/install-hooks.sh` so it is
    opt-in and versioned, don't write into `.git/` directly;
  - if the project uses Claude Code, a `PreToolUse`/`PostToolUse` hook in
    `.claude/settings.json` invoking the same scripts for edits touching guarded paths.
  Local hooks are convenience; CI is authority. A check that exists only locally counts as
  NOT enforced (anti-pattern #7).

### Phase 4 — Red-first proof

For each guard/test: sabotage → run → capture the failing output → revert sabotage → run
again green. Record a one-line evidence entry (what was broken, what went red) per rule in
`.harness/invariants.md`. A rule without red evidence must be listed under "UNPROVEN" in the
final summary.

Special cases (define them, don't improvise):

- **Greenfield / code not written yet**: a test written before the implementation is
  naturally red — run it, capture the red output, and record evidence as
  `red-by-construction (awaiting implementation)`. That counts. What does NOT count is
  skipping the run.
- **Static guards — match the sabotage to what the guard reads.** Content guards (grep over
  the working tree) can be sabotaged with an uncommitted scratch edit, reverted with
  `git checkout --`. Diff-based guards compare `BASE_REF...HEAD` — an uncommitted change is
  INVISIBLE to them and produces a fake-green proof. Sabotage those with a real commit on a
  throwaway branch, run the guard there, then delete the branch. Either way, verify
  `git status` is clean and you are back on the working branch before proceeding.
- **Cannot sabotage safely** (e.g. rule about prod-only infra): mark UNPROVEN with the
  reason. Never fabricate evidence.

### Phase 5 — Meta-rules + handoff

Install the anti-cheating layer:

- `CODEOWNERS` entry locking `.harness/`, `scripts/harness/`, the harness test dirs, and the
  CI workflow to human reviewers (template: `references/templates/CODEOWNERS.tpl`).
- Local bypasses (`--no-verify`, deleted hooks) leave no trace CI can detect — do NOT try to
  build a check for them. The actual defense is the CI mirror: CI re-runs the same scripts,
  so bypassing local hooks changes nothing. What CI *can* detect: `[skip ci]` markers in
  commit messages (fail the harness-integrity gate on them) and modifications to harness
  paths (flag for CODEOWNERS review). Branch protection must be set by the human — say so
  in the summary; you cannot set it yourself.
- Coverage gate config if the project has coverage tooling; never lower an existing threshold.

**Self-audit (mandatory before handoff):** walk EVERY enforcer you produced through the
9 rejection patterns in `references/anti-patterns.md` and record the verdict per enforcer.
Any enforcer that trips a pattern gets fixed or downgraded to "documented only" — it does
not ship as protection. Include the audit table in the handoff.

Then STOP and hand off using `references/templates/handoff.md.tpl`. It must contain:

1. Table of rules: ID · statement · source tag · tier · red evidence (or UNPROVEN).
2. Explicit list of every `[AI-INFERRED]` rule awaiting confirmation.
3. Self-audit table (enforcer × anti-pattern verdict).
4. What the human must do: review checklist, enable branch protection, confirm CODEOWNERS.
5. What you deliberately did NOT do (merge, self-approve, weaken existing rules).

## Scope discipline

- New/greenfield project: it is normal that Phase 3 produces only a walking-skeleton gate
  plus 2–3 highest-risk invariants. Do not fabricate tests for code that doesn't exist.
- Existing project: do not "fix" failing code to make your new tests pass in the same run —
  a new invariant test that fails against current code is a FINDING; report it, don't silently
  patch production code or water down the test.
- Never edit anything under an existing `.harness/` or `scripts/harness/` except to ADD;
  modifications require the human path.

## After handoff — the harness is sediment, not a monument

Creation is one-time; accretion is forever. The handoff must tell the team (and future
agents) the two standing rules that keep the harness alive:

- **Every production bug → one regression rule.** Before (or with) the fix, add a ledger
  entry + enforcer that would have caught it, red-proofed against the unfixed code. A bug
  without a rule will return.
- **Every new feature with a Vein-1 surface (money/authz/destruction/irreversibility) →
  its invariant lands with the feature**, not later. Re-running `/create-harness` performs
  gap-analysis (Phase 0) when drift is suspected.
