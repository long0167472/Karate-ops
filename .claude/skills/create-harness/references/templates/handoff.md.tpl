# Harness Draft — Approval Request: {{PROJECT}}

> Status: DRAFT on branch `{{BRANCH}}`. Nothing is merged. Nothing below is "done" until a
> human approves it. Mode: {{greenfield | gap-analysis}}.

## 1. Rule ledger

| ID | Rule | Source | Tier | Enforcer | Red evidence |
|----|------|--------|------|----------|--------------|
| INV-001 | | [HUMAN] | 3 | | ✅ {{date, what was sabotaged → observed failure}} |
| INV-002 | | [AI-INFERRED] | 1 | | ⛔ UNPROVEN — {{reason}} |

## 2. Awaiting your confirmation ([AI-INFERRED])

- [ ] INV-0XX: {{statement}} — inferred from {{where}}. Confirm, amend, or reject.

## 3. Self-audit vs anti-patterns

| Enforcer | Vacuous | Mirror | Frozen bug | Overmocked | Verdict |
|----------|---------|--------|------------|------------|---------|
| {{test/guard}} | ok | ok | ok | ok | SHIPS |
| {{test/guard}} | ok | ⚠️ {{note}} | ok | ok | DOWNGRADED to documented-only |

## 4. Promotion candidates (warn → block)

| Rule | Shipped as | Why not block yet | Promote when |
|------|-----------|-------------------|--------------|
| {{INV-0XX}} | warn | {{e.g. doc-sourced, unconfirmed}} | {{e.g. 2 weeks soak, zero false positives + your sign-off}} |

## 5. Findings (things the harness surfaced, NOT silently fixed)

- {{e.g. INV-003 fails against current code — current behavior may be a live bug.}}

## 6. What you (human) must do

- [ ] Review every rule above; resolve section 2.
- [ ] Enable branch protection on `{{DEFAULT_BRANCH}}`: require harness gates + CODEOWNERS review.
- [ ] Confirm CODEOWNERS entries name the right humans.
- [ ] Run `scripts/harness/install-hooks.sh` locally (and tell the team to).

## 7. What was deliberately NOT done

- Not merged; not self-approved; no existing rules/thresholds modified or weakened
- {{anything else skipped, with reason}}
