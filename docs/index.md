# Documentation Index

Run21AI documentation follows a single-owner rule: each topic is explained in one document, while other files link to it instead of repeating the same contract or procedure.

## Start Here

- [Installation](INSTALLATION.md) — prerequisites, build setup, REST profiles, Ollama, Rust, and Python setup.
- [Usage](USAGE.md) — REST, Kotlin, formula tool, prompt dispatch, and `run21ctl` examples.
- [Testing](TESTING.md) — Kotlin, Rust, pytest, and optional external-model validation.

## Architecture

- [Architecture](architecture/ARCHITECTURE.md) — module boundaries, dependency direction, registration, deployment profiles, and quality attributes.
- [Execution Contracts](architecture/EXECUTION.md) — canonical pipeline, models, dispatcher outcomes, registries, validation, errors, and audit metadata.
- [LLM Dispatch](architecture/LLM_DISPATCH.md) — LLM boundary, Ollama adapter, structured-output ownership, validation, and safety rules.

## Project Management

- [Roadmap](ROADMAP.md) — future work only.
- [Changelog](CHANGELOG.md) — completed repository-level changes.
- [Development Rules](../AGENTS.md) — repository-wide engineering and documentation rules.

## Machine-Readable Contracts

- [REST OpenAPI contract](../runtime/rest-api/src/main/resources/openapi.yaml)
- [Ollama formula-dispatch schema](../dispatchers/llm-ollama/src/main/resources/ollama/formula-dispatch/output-schema.json)

Module-local README files may provide entry commands and link back to these owning documents. Nested navigation-only indexes should not duplicate this file.
