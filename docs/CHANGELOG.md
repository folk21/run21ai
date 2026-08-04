# Changelog

This file records completed repository-level changes. Future work belongs in [Roadmap](ROADMAP.md).

## Unreleased

### Changed

- Protected caller provenance in `OrderedDispatcherChain`; dispatchers can no longer replace
  actor, source, or correlation values during enrichment or request acceptance.
- Reduced the documentation hierarchy to a compact set of owning documents.
- Replaced repeated architecture, core, tool, API, LLM, testing, and client explanations with links to their owners.
- Reorganized architecture documentation into `ARCHITECTURE.md`, `EXECUTION.md`, and `LLM_DISPATCH.md` under `docs/architecture/`.
- Refocused the root README on project purpose, current capabilities, quick start, and navigation.
- Refocused `AGENTS.md` on development rules and documentation ownership.
- Moved the roadmap to `docs/ROADMAP.md` and added `docs/index.md` as the central documentation index.
- Replaced the architecture module-boundary text diagram with Mermaid.
- Replaced the stage-history roadmap with compact per-item `[x]` `DONE`, `[/]` `IN_PROGRESS`, and `[ ]` `TODO` status markers.
- Consolidated REST, Kotlin API, formula tool, runtime profile, and Rust CLI examples in `docs/USAGE.md`.
- Consolidated Kotlin, Rust, and pytest validation commands in `docs/TESTING.md`.

### Removed

- Removed numbered topic folders and nested navigation-only indexes.
- Removed duplicated OpenAPI and LLM structured-output schema copies from `docs/`; runtime resources remain the machine-readable sources of truth.
- Removed the standalone project-instructions document because repository-wide engineering rules are owned by `AGENTS.md`.
