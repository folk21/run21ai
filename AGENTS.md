# AGENTS.md

This file defines repository-wide development rules for Run21AI. Read the nearest local documentation before changing a concrete tool, runtime, dispatcher adapter, client, or test package.

## Project Intent

Run21AI is a Kotlin-first dispatcher-driven tool execution engine. The stable architectural rule is:

> Dispatchers prepare `ToolRequest` values. `ToolImplementation` objects perform real work.

The detailed contracts are owned by [Execution Contracts](docs/architecture/EXECUTION.md). Do not restate them in new documents or module comments.

## Source of Truth

Use the following ownership order:

1. production code and machine-readable schemas define implemented behavior;
2. tests define verified behavior;
3. `docs/architecture/` explains stable architecture and contracts;
4. `docs/USAGE.md` explains supported commands and runtime configuration;
5. `docs/ROADMAP.md` contains future work only;
6. `docs/CHANGELOG.md` records completed documentation and release changes.

When documentation conflicts with code, verify the behavior, fix the owning document, and replace duplicate explanations with links.

## Architecture Rules

- Keep `:core` framework-independent.
- Keep dependencies pointing from runtimes, adapters, and tools toward core contracts.
- Keep Ktor, EvalEx, Ollama, persistence, CLI frameworks, and external service SDKs outside `:core`.
- Route every execution through a `DispatcherChain`.
- Use `PassThroughDispatcher` for already structured requests; do not create a second direct-execution path.
- Keep LLM integrations outside `:core` and outside tool implementations.
- Never let an LLM calculate the final business result or bypass `ToolExecutor`, audit metadata, validation, or runtime policy.
- Treat calculation-like behavior as a tool, not as a special execution subsystem.
- Prefer explicit code-based registration over reflection or implicit classpath discovery.
- Add abstractions only after at least two concrete use cases require them.

## Terminology

Use these names consistently:

- `Dispatcher`
- `DispatcherChain`
- `DispatchInput`
- `StructuredDispatchInput`
- `RawDispatchInput`
- `ToolRequest`
- `ToolExecutor`
- `ToolImplementation`
- `ToolExecutionOutput`
- `ToolResult`
- `ToolError`
- `AuditMetadata`
- `ToolCalls`

Do not introduce parallel terms for the same concepts.

## Kotlin Rules

- Use idiomatic Kotlin and immutable data classes.
- Prefer small explicit interfaces and constructor injection.
- Use sealed types or enums for controlled states.
- Avoid reflection, global mutable state, and hidden registration.
- Keep public APIs stable and self-explanatory.
- Use KDoc for public contracts and important public functions.
- Add inline comments only for non-obvious decisions.
- Keep all code comments, KDoc, configuration comments, examples, and software documentation in English.

## Core Changes

Before changing `core/src/main/kotlin`:

- inspect the relevant contract tests in `core/src/test/kotlin`;
- preserve framework independence;
- preserve the single execution pipeline;
- return structured failures for expected unsupported or invalid states;
- update tests for every semantic change;
- update [Execution Contracts](docs/architecture/EXECUTION.md) only when the public contract changes.

Prefer `ToolCalls.execute(...)` for structured contract scenarios and `ToolCalls.dispatch(...)` for raw-input scenarios. Use lower-level models directly only when testing dispatchers, registries, validation, builders, or unsupported defaults.

## Tool and Runtime Changes

- Keep tool-specific parsing, validation, and execution inside the tool module.
- Keep runtime composition outside tool implementations and outside `:core`.
- Keep HTTP DTOs and mapping in the REST runtime.
- Keep provider-specific LLM transport and prompt resources in the provider adapter module.
- Register a new tool through the existing core registration model before adding new registration abstractions.
- Update the OpenAPI resource and REST tests together when the HTTP contract changes.

## LLM Changes

- Treat model output as untrusted input.
- Validate structured output before creating or executing a `ToolRequest`.
- Do not let a model invent missing business values.
- Keep retries bounded and deterministic when retry support is added.
- Record useful provider, model, prompt, and attempt metadata without logging sensitive payloads.
- Keep provider-neutral behavior in `:dispatchers:llm`; keep Ollama-specific behavior in `:dispatchers:llm-ollama`.

The current LLM boundary and schema ownership are documented in [LLM Dispatch](docs/architecture/LLM_DISPATCH.md).

## Testing

Use the smallest relevant test layer first:

```bash
./gradlew :core:test
./gradlew :tools:formula:test
./gradlew :dispatchers:llm:test :dispatchers:llm-ollama:test
./gradlew :runtime:rest-api:test
./gradlew test
```

Run Rust and pytest checks when changing the REST contract or clients. The complete command matrix is owned by [Testing](docs/TESTING.md).

Tests must be deterministic. Core tests must not require external services, credentials, an LLM server, or a network connection.

## Documentation Rules

Each topic has one owning document:

| Topic | Owner |
|---|---|
| Project overview and quick start | `README.md` |
| Engineering rules | `AGENTS.md` |
| Documentation navigation | `docs/index.md` |
| Architecture boundaries | `docs/architecture/ARCHITECTURE.md` |
| Execution models and contracts | `docs/architecture/EXECUTION.md` |
| LLM dispatch design | `docs/architecture/LLM_DISPATCH.md` |
| Commands and runtime configuration | `docs/USAGE.md` |
| Test commands and coverage layers | `docs/TESTING.md` |
| Future work | `docs/ROADMAP.md` |
| Completed changes | `docs/CHANGELOG.md` |
| HTTP schema | `runtime/rest-api/src/main/resources/openapi.yaml` |
| Ollama output schema | `dispatchers/llm-ollama/src/main/resources/ollama/formula-dispatch/output-schema.json` |

Use `docs/index.md` as the only central documentation index. Do not create nested navigation-only Markdown files or copy machine-readable schemas into `docs/`; link to the owning file instead.

## Change Scope

- Make the smallest useful change.
- Preserve behavior unless the task explicitly changes it.
- Avoid unrelated modules and speculative framework integration.
- Update tests and the owning documentation in the same change.
- Mention changed files and validation performed.
- Preserve the repository root name `run21ai/` in generated archives.
