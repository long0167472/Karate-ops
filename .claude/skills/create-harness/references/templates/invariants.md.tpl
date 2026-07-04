# {{PROJECT}} — Invariant Ledger

> Source of truth for hard rules. Every rule here is ENFORCED (or explicitly marked not yet).
> Changes to this file require human review (see CODEOWNERS).
> Source tags: [HUMAN] confirmed by a person · [DOC:path] from a document · [AI-INFERRED] unconfirmed.

| ID | Rule (testable statement) | Why (consequence if violated) | Source | Tier | Enforcer | Red evidence |
|----|---------------------------|-------------------------------|--------|------|----------|--------------|
| INV-001 | {{e.g. Server computes all billing amounts; client payloads must not set them}} | {{e.g. wrong charges to real users}} | [HUMAN] | 3 | {{path/to/TestClass#method}} | {{date: broke X, test failed with Y}} |
| INV-002 | | | | | | |

## Unconfirmed rules ([AI-INFERRED] — awaiting human decision)

- INV-0XX: {{statement}} — found in {{where}}, needs confirmation before it can be trusted.

## Documented-only rules (no enforcer yet — visible debt, not silent)

- {{rule}} — reason no enforcer exists yet, planned tier.

## Meta-rules (protect the harness itself)

- META-1: `.harness/`, `scripts/harness/`, harness test dirs, and CI workflow are CODEOWNERS-locked.
- META-2: Gates may only turn green by fixing code or by a human changing the rule — never by
  weakening the gate. (anti-patterns.md #6)
- META-3: Every check runs identically locally and in CI (same script — CI mirror).
- META-4: Coverage thresholds may rise, never fall, without human sign-off.
