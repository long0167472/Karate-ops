# CODEOWNERS — lock the harness to human reviewers.
# The agent that writes code must not be able to approve changes to the rules that judge it.
# Add these entries to .github/CODEOWNERS (create the file if absent).
#
# ORDERING MATTERS: GitHub uses LAST-match-wins. These entries must appear AFTER any
# broad patterns (e.g. `* @team`) in the file — a later `*` line would silently override
# every lock below. When appending to an existing CODEOWNERS, put this block at the END.

/.harness/                          {{@HUMAN_OWNERS}}
/scripts/harness/                   {{@HUMAN_OWNERS}}
/.github/workflows/harness.yml      {{@HUMAN_OWNERS}}
/.github/CODEOWNERS                 {{@HUMAN_OWNERS}}
{{/path/to/harness/test/dir/}}      {{@HUMAN_OWNERS}}
# If the project uses Claude Code hooks as the local mirror, the hook config is part of
# the harness too — an agent that can edit it can un-wire its own guardrails:
/.claude/settings.json              {{@HUMAN_OWNERS}}

# Reminder to the human: CODEOWNERS only bites if branch protection requires
# "Review from Code Owners" on the default branch. Enable it.
