# CODEOWNERS — lock the harness to human reviewers.
# The agent that writes code must not be able to approve changes to the rules that judge it.
# Add these entries to .github/CODEOWNERS (create the file if absent).

/.harness/                          {{@HUMAN_OWNERS}}
/scripts/harness/                   {{@HUMAN_OWNERS}}
/.github/workflows/harness.yml      {{@HUMAN_OWNERS}}
/.github/CODEOWNERS                 {{@HUMAN_OWNERS}}
{{/path/to/harness/test/dir/}}      {{@HUMAN_OWNERS}}

# Reminder to the human: CODEOWNERS only bites if branch protection requires
# "Review from Code Owners" on the default branch. Enable it.
