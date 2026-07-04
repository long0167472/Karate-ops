# Harness Anti-Patterns — Rejection List

Reject (or flag, never silently ship) any enforcer matching these. These are the known ways
an AI-generated harness becomes theater: it looks like protection but protects nothing.

## 1. Vacuous test
Asserts nothing that can fail: `assert(true)`, everything mocked including the subject,
no assertion after the act, try/catch swallowing the failure.
**Detector:** the red-first run. If you cannot make it fail by sabotaging the rule, it is vacuous.

## 2. Mirror / tautological test
Computes the expected value by calling the same code under test, or snapshots current
behavior and calls the snapshot "the spec".
**Detector:** ask "can I state the expected value without reading the implementation?"
If no → you have a snapshot, not a rule. Flag: "needs a real spec from the human."

## 3. Frozen bug
Writing tests that encode CURRENT behavior of code that is actually wrong, thereby promoting
a bug to a protected invariant.
**Detector:** any invariant test that passes trivially against current code deserves the
question "is current behavior actually correct?" — if unsure, tag `[AI-INFERRED]`.

## 4. Happy-path stuffing
Twenty tests on the easy CRUD path, zero on money/authz/data-loss.
**Detector:** the rule ledger must be ordered risk-first; if tier-3 tests exist for low-risk
rules while a catastrophic rule is unenforced, stop and fix the order.

## 5. Overmocked integration
An "integration" test where every collaborator is mocked — it tests the mocks.
Mock only true externals (clock, network, third-party APIs).

## 6. Weakening disguised as fixing
Making a red gate green by editing the gate (loosening the assertion, adding the file to an
ignore list, lowering the coverage threshold). The ONLY legitimate ways to turn a gate green
are: fix the code, or get a human to change the rule.

## 7. Local-only enforcement
A check that exists only in a git hook or only in the editor. Anything not mirrored in CI
does not exist — agents (and humans) can delete local hooks.

## 8. Natural-language-only rule
A rule that lives only in CLAUDE.md/context docs with no executable enforcer, presented as
"covered". Context is advice; the harness is enforcement. A rule without an enforcer belongs
in the ledger with enforcement tier "NONE — documented only", visibly.
