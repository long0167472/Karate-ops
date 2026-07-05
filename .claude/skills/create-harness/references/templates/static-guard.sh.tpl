#!/usr/bin/env bash
# {{RULE_ID}}: {{one-line statement of the rule}}
# Tier 1 static guard. Runs identically in local hooks and CI (CI mirror).
# Exit 0 = pass, exit 1 = violation. Always name the rule ID in the failure message.
# FILENAME ENCODES SEVERITY: name this file block-<rule>.sh (fails the gate) or
# warn-<rule>.sh (reported, non-blocking). The CI loop dispatches on the prefix.
set -euo pipefail

BASE_REF="${BASE_REF:-origin/{{DEFAULT_BRANCH}}}"

# ── Example A: forbid a pattern in source ─────────────────────────────
# if grep -rnE '{{FORBIDDEN_PATTERN}}' {{SRC_DIRS}} --include='{{GLOB}}' \
#      | grep -vE '{{WHITELIST_PATTERN}}'; then
#   echo "❌ {{RULE_ID}} violated: {{message}} (see .harness/invariants.md)" >&2
#   exit 1
# fi

# ── Example B: forbid modifying protected files (diff-based) ──────────
# Allows ADDING new files; blocks MODIFYING or DELETING existing ones (--diff-filter=MD).
# modified=$(git diff --diff-filter=MD --name-only "$BASE_REF"...HEAD | grep -E '{{PROTECTED_PATH_REGEX}}' || true)
# if [ -n "$modified" ]; then
#   echo "❌ {{RULE_ID}} violated: protected files modified:" >&2
#   echo "$modified" >&2
#   exit 1
# fi

echo "✅ {{RULE_ID}} pass"
