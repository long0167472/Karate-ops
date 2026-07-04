# .github/workflows/harness.yml — fail-fast gate chain.
# Gate order: cheap → expensive. A red gate stops the chain.
# Adapt job steps to the project's real toolchain; keep the structure.
name: harness

on:
  pull_request:
  push:
    branches: [{{DEFAULT_BRANCH}}]

jobs:
  gate-1-static-guards:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with: { fetch-depth: 0 }   # guards diff against the base branch
      - name: Run static guards (same scripts as local hooks — CI mirror)
        run: |
          set -e
          for g in scripts/harness/*.sh; do
            echo "── $g"
            bash "$g"
          done

  gate-2-compile:
    needs: gate-1-static-guards
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      # {{e.g. setup-java + mvn -q compile / setup-node + npm ci && npm run check}}

  gate-3-invariant-tests:
    needs: gate-2-compile
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      # {{e.g. mvn test / npm test — must include the harness/ test package}}
      # {{coverage gate: fail if below .harness/coverage-gate.json thresholds}}

  gate-4-harness-integrity:
    # Meta-gate: harness files changed? → require the human-review label / CODEOWNERS.
    needs: gate-1-static-guards
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with: { fetch-depth: 0 }
      - name: Detect harness modifications
        run: |
          base="${{ github.event.pull_request.base.sha || 'HEAD~1' }}"
          if git diff --name-only "$base"...HEAD | grep -E '^(\.harness/|scripts/harness/|\.github/workflows/harness\.yml)'; then
            echo "::warning::Harness files modified — CODEOWNERS review is required."
          fi

# NOTE for the human (the skill cannot do this itself):
# 1. Enable branch protection on {{DEFAULT_BRANCH}}: require all gates + CODEOWNERS review.
# 2. Disallow force-push and workflow-file edits without review.
