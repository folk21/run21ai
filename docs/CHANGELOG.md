# Changelog

This file records completed repository-level changes. Future work belongs in [Roadmap](../ROADMAP.md).

## Unreleased

### Changed

- Reduced the documentation hierarchy to a compact set of owning documents.
- Replaced repeated architecture, core, tool, API, LLM, testing, and client explanations with links to their owners.
- Reorganized architecture documentation into overview, execution-contract, and LLM-dispatch documents under `docs/architecture/`.
- Refocused the root README on project purpose, current capabilities, quick start, and navigation.
- Refocused `AGENTS.md` on development rules and documentation ownership.
- Replaced the stage-history roadmap with prioritized `Now`, `Next`, and `Later` work.
- Consolidated REST, Kotlin API, formula tool, runtime profile, and Rust CLI examples in `docs/USAGE.md`.
- Consolidated Kotlin, Rust, and pytest validation commands in `docs/TESTING.md`.

### Removed

- Removed navigation-only documentation indexes and numbered topic folders.
- Removed duplicated OpenAPI and LLM structured-output schema copies from `docs/`; runtime resources remain the machine-readable sources of truth.
- Removed the standalone project-instructions document because repository-wide engineering rules are owned by `AGENTS.md`.
