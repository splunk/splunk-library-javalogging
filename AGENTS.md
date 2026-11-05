# AGENTS.md

## Responses

- Output tokens are precious, be succinct in your responses. Use ASD-STE100
  Simplified Technical English.
- Do not anthropomorphize yourself.
- Ask before coding. Sometimes a rubber duck is good enough.

## Code

- Run the project's linter/formatter before considering code done. Fix type
  errors, unused imports, lint violations.
- After modifying GitHub workflows, verify their security with zizmor (e.g. `uvx zizmor --no-ignores --persona=auditor`).
- In GitHub Workflows, [`$/` means repo root](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax#example-using-an-action-in-the-same-repository-as-the-workflow-at-the-running-commit-recommended)

## Scope

- Fix what was asked. No unrelated refactors, docstrings, or cleanup.
- Add comments only for non-obvious logic.
- Ask before adding dependencies or touching infrastructure code (CI/CD,
  Dockerfiles, deployment configs).

## Safety

- Confirm before deleting files, force-pushing, dropping database tables,
  changing CI/CD, or affecting shared state.
- Prefer reversible steps: stage before committing, branch before rebasing.
- Never use --no-verify or bypass git hooks without explicit instruction.
