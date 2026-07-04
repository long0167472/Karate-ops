# /create-harness Skill — Current Status

**Last updated:** 2026-07-04 (Commit 6d9c679)  
**Lines of code:** 610 (core logic + templates)  
**Maturity level:** Beta — ready for trial run, 5 hardening iterations complete  
**Test mode:** Phase 0 (idempotency) through Phase 5 (handoff) structurally sound; Phases 4–5 not yet exercised against real projects

## Architecture

```
.claude/skills/create-harness/
├── SKILL.md                      [176 lines] Core 6-phase process + 6 non-negotiable laws
├── STATUS.md                     [this file] Skill state & readiness
└── references/
    ├── anti-patterns.md          [54 lines] 9 rejection patterns (vacuous, mirror, flaky, etc)
    ├── interview-questions.md    [36 lines] 7 interview rounds (evidence-backed, risk-first)
    ├── recon-playbook.md         [98 lines] 5 veins for mining unfamiliar codebases
    └── templates/
        ├── invariants.md.tpl     [27 lines] Rule ledger template (ID, rule, source tag, severity)
        ├── rules.yaml.tpl        [20 lines] Machine-readable rule map
        ├── harness.yml.tpl       [68 lines] CI gate chain (static → compile → test → coverage)
        ├── static-guard.sh.tpl   [28 lines] Tier-1 guard scaffold (block-*/warn-* prefixes)
        ├── CODEOWNERS.tpl        [19 lines] Harness lock (last-match-wins warning)
        └── handoff.md.tpl        [47 lines] Approval request template (7 sections)
```

## Key Design Decisions (WHY)

| Decision | Rationale | Proven? |
|----------|-----------|---------|
| **6 phases** | Idempotency + interview + ledger + enforcement + red-proof + meta-rules | ✅ Phase 0 (gap-analysis mode) + Phases 1–3 structure sound; 4–5 awaiting real use |
| **Severity by filename** (`block-*.sh` / `warn-*.sh`) | CI dispatch without doubling code; silently-skipped guards caught by lint | ✅ Executable proof (gate loop tested red/green) |
| **No [AI-INFERRED] rules can ship at `block`** | Prevents AI from encoding bugs as protected rules; soak → promotion path for confidence | ⚠️ Awaiting human confirmation in real use |
| **Vein 0 (context base) before Veins 1–5** | Distilled docs > interview > code mining; staleness-check gates trust | ✅ Applied to KarateOps, found `context/known-issues/` immediately |
| **Red-first proof mandatory** | Tests that never fail prove nothing; execution catches vacuous/flaky/mirror patterns | ✅ Loop iter 4 proven; awaiting tier-3 test proof |
| **Ledger + source tags** | Separates knowledge (what is a rule) from approval (does it block); decouples human bandwidth | ⚠️ Awaiting real handoff |

## Hardening Log (5 iterations)

| Iter | Finding | Fix | Severity |
|------|---------|-----|----------|
| 1 | CI can't detect `--no-verify` (local flag) | Rewrote: CI mirror re-running same scripts IS the defense | 🔴 Breaking (wrong model) |
| 2 | CODEOWNERS last-match-wins silently overrides harness locks | Template warning + lock `.claude/settings.json` too | 🔴 Breaking (silent hole) |
| 3 | Flaky gates train everyone to disbelieve harness | Added anti-pattern #8, 3× stability run before shipping | 🟠 High (trust erosion) |
| 4 | Unprefixed guards silently skipped in CI | Added lint: `find ... ! -name 'block-*' ! -name 'warn-*'` | 🔴 Breaking (silent failure) |
| 5 | Fresh clone has no git history (Claude Code web sessions) | Vein 2: check `rev-list --count`, unshallow or downgrade | 🟠 High (wrong conclusions) |

**Defect rate:** 5 finds / 5 iterations = stable (each find is distinct, no repeat categories).

## Known limitations

1. **Phase 4 (red-first) not tested against real test frameworks** — guidance says "use framework conventions" but only shell scaffolds are proven. Will need example Java/Node tests from KarateOps trial run.

2. **Phase 5 handoff checklist is unopened** — no human has yet said "yes, I'll sign off on these rules" and triggered the soak→promote flow. The path exists, behavior TBD.

3. **Vein 4 (existing tests) surface coverage unknown** — skill assumes project has tests; greenfield projects with zero tests will find Vein 4 empty and incorrectly assume "no gaps to fill."

4. **Meta-rule about GitHub branch protection** — skill says "you must enable it; I cannot" — but the handoff should call out what happens if the human forgets (PR merges without harness review). Needs a follow-up check step in the human's weekly routine.

5. **Coverage gate (Phase 5)** — template assumes coverage tooling exists; projects with none will skip this. No explicit "no coverage tool detected" path in the instructions.

## Trial run checklist (next step)

When `/create-harness` runs on KarateOps:

- [ ] **Phase 0 idempotency** — no `.harness/` exists; detects this, runs greenfield mode
- [ ] **Recon Vein 0** — mines `context/`, finds `known-issues/billing-cycle-leak.md` as `[DOC]` sourced
- [ ] **Recon Veins 1–5** — surfaces `billing/amount`, `permission/organizationId`, soft-delete rule, hotspot `MatchServiceImpl`
- [ ] **Interview round 3** — asks "which rules block-merge?" for promotion decision
- [ ] **Phase 2 ledger** — 6 Critical Rules from CLAUDE.md lands as [DOC:path] + [AI-INFERRED] mix
- [ ] **Phase 3 guards** — produces `block-no-migration-edit.sh`, `block-no-physical-delete.sh`, maybe `warn-publish-check.sh`
- [ ] **Phase 3 tests** — scaffolds invariant test classes (doesn't implement, just names them)
- [ ] **Phase 4 red-proof** — guides through sabotaging one guard/test, capturing the red output
- [ ] **Phase 5 handoff** — produces `.harness/invariants.md` with a promotion table + checklist
- [ ] **No auto-merge** — skill produces a draft PR, human must approve

## Readiness assessment

**Core skill:** ✅ Ready  
- Process is sound (6 phases, fail-safe defaults, single point of human decision)
- Anti-patterns are real (9 types, each with detection method)
- Recon veins are prioritized (context base first, fresh-clone edge case handled)

**Templates:** ✅ Ready  
- All syntactically valid (bash-n passed, YAML parses, Markdown renders)
- Demonstrated mechanics (gate loop tested red/green, lint catches silent holes)
- Documented gotchas (CODEOWNERS last-match-wins, guard filename dispatch)

**Documentation:** ⚠️ Adequate  
- Clear for an expert reading soup-to-nuts
- May need run-along example for first-timer (no walkthrough of actual output)

**Execution (the skill running):** ⚠️ Not yet tested  
- Phase 1 interview logic not exercised (AskUserQuestion calls, parsing answers)
- Phase 4 red-proof GIT mechanics (commit/revert on throwaway branch) not proven
- Phase 5 handoff file writes not traced

**Next:** Run `/create-harness` on KarateOps → expose what's left unsaid, iterate on feedback.
