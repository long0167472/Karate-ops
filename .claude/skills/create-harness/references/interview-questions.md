# Invariant Elicitation Questions

Ask via AskUserQuestion, batched 2–4 per call. Adapt wording to the project; keep the intent.
Mine `CLAUDE.md`, docs, ADRs, known-issues, and existing tests FIRST so options you offer are
concrete, not generic.

## Round 1 — Catastrophe map (always ask)

1. **"What, if it goes wrong in production, is a disaster rather than a bug?"**
   Offer concrete candidates found in the codebase (e.g. wrong billing amounts, cross-tenant
   data leaks, lost records, irreversible deletes). MultiSelect.
2. **"Which of these rules must survive ANY refactor?"**
   List the candidate invariants you mined, each phrased as a testable statement.
   The human's selections become `[HUMAN]`; the rest stay `[AI-INFERRED]`.

## Round 2 — Boundaries and history

3. **"Where is the ownership boundary for business logic?"**
   (e.g. server computes X, client must send null; layer A may not import layer B.)
4. **"What bug already happened that a gate should have caught?"**
   Every answer becomes a regression invariant with source `[HUMAN]` — these are the
   highest-value rules because they are proven failure modes.

## Round 3 — Enforcement posture

5. **"Who must approve changes to the harness itself?"** → CODEOWNERS entries.
6. **"Is there code that is explicitly OUT of scope for hard gates?"**
   (prototypes, spikes, generated code) → exclusion list, so the harness doesn't rot into
   something people bypass wholesale.

## Rules for the interview

- Never present an inferred rule as fact; phrase as "I found X in <file> — is this a hard rule?"
- If the human answers "I don't know" → the rule stays `[AI-INFERRED]`, tier still assigned,
  listed in the unconfirmed section. Do not drop it silently.
- If the human is unavailable, complete Phases 2–4 with everything tagged `[AI-INFERRED]`
  and make the final summary lead with the unconfirmed list. Never skip the tagging.
