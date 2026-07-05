# /create-harness Skill — Status Log

**Last updated:** 2026-07-04 · **Maturity:** Beta — structure hardened over 6 review
iterations; Phases 4–5 not yet exercised end-to-end against a real project.

> Anti-rot rule for this file: no per-file line counts or other numbers that decay with
> every edit (`wc -l` when you need them). Log decisions, defects, and limits only.

## Contents (roles, not line counts)

- `SKILL.md` — 6 non-negotiable laws + 6-phase process (detect → interview → ledger →
  enforcers → red-proof → meta/handoff)
- `references/anti-patterns.md` — 9 rejection patterns: vacuous, mirror, frozen bug,
  happy-path stuffing, overmocked, weakening-as-fixing, local-only, flaky gate,
  natural-language-only
- `references/interview-questions.md` — 7 questions in 3 rounds (catastrophe map,
  boundaries & history, enforcement posture incl. the block-merge promotion decision)
- `references/recon-playbook.md` — 6 veins: context base (0, mine first), money/authz
  surface (1), git scar record (2, fresh-clone check), schema (3), tests (4), hotspots (5)
- `references/templates/` — invariants ledger, rules.yaml, harness.yml CI gate chain,
  static-guard.sh (block-*/warn-* dispatch), CODEOWNERS (last-match-wins warning),
  handoff approval doc

## Hardening log

| Iter | Defect found | Class |
|------|--------------|-------|
| 1 | CI "detects --no-verify" was impossible; `warn` severity had no runtime mechanism | wrong model / dead policy |
| 2 | CODEOWNERS last-match-wins silently voids harness locks; hook config unlocked | silent bypass |
| 3 | No flaky-gate anti-pattern; no post-handoff accretion rules | trust erosion / lifecycle gap |
| 4 | Unprefixed guards silently skipped (found by EXECUTING the gate loop, not reading it) | silent skip |
| 5 | Fresh clone ⇒ empty scar record ⇒ "no past bugs" false conclusion (found by dry-running recon on this repo) | env blind spot |
| 6 | STATUS.md itself shipped stale counts, an 8-of-9 list, wrong round count | doc rot |

Method lesson: iterations 1–3 came from critical re-reading; 4–6 from executing/dry-running
the artifacts. Execution finds strictly better defects — prefer it.

## Known limitations (open)

1. Phase 4 red-proof unproven against a real test framework (only shell guards exercised).
2. Phase 5 handoff → human sign-off → soak → warn-to-block promotion flow never run.
3. Vein 4 assumes tests exist; zero-test projects get no explicit "empty vein" guidance.
4. Branch protection depends on the human enabling it; no follow-up check if they forget.
5. No "coverage tooling absent" path — Phase 5 just skips coverage silently.

## Next step

Trial run `/create-harness` on KarateOps (greenfield mode — no `.harness/` exists yet):
recon should surface the 6 Critical Rules from CLAUDE.md via Vein 0
(`context/known-issues/billing-cycle-leak.md` etc.); the run must stop at Phase 5 with a
handoff doc, an unmerged draft, and the block-merge question answered by a human.
