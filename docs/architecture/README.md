# Architecture

Run21AI separates request preparation from deterministic tool execution. The root [README](../../README.md) contains the project-level flow; this directory owns architectural boundaries and contracts.

Architecture documents:

- [Execution Contracts](EXECUTION.md) — canonical models, dispatcher semantics, execution, errors, audit, and registration.
- [LLM Dispatch](LLM_DISPATCH.md) — provider-neutral LLM boundary, Ollama adapter, schema ownership, and safety rules.

## Architectural Goals

- one execution pipeline for structured and raw input;
- framework-independent core contracts;
- deterministic tool work outside dispatchers;
- explicit registration and dependency direction;
- structured errors and auditable results;
- optional LLM integration without coupling execution to a provider;
- small public APIs and minimal framework ceremony.

## Module Boundaries

```text
clients/ and external callers
        |
        v
runtime/                 transport and application composition
        |
        +----> dispatchers/   request preparation adapters
        |
        +----> tools/         concrete executable capabilities
        |
        v
core/                    framework-independent contracts and semantics
```

Dependencies point toward `core`. Core must not import runtime, tool, dispatcher-provider, persistence, CLI, or external-service implementations.

| Module | Owns | Must not own |
|---|---|---|
| `:core` | Models, dispatcher contracts, execution semantics, registries, audit models, unsupported defaults, high-level call API | Ktor, EvalEx, Ollama, persistence, transport DTOs, deployment configuration |
| `:tools:*` | Tool descriptor, validation, implementation, and tool-local wiring | REST routes, LLM prompt construction, runtime profile selection |
| `:dispatchers:llm` | Provider-neutral LLM request/response and dispatcher mapping | Provider transport, tool execution |
| `:dispatchers:llm-ollama` | Ollama configuration, transport, prompts, schema, and response normalization | Core execution or business results |
| `:runtime:local` | Lightweight in-memory application composition | Core semantics |
| `:runtime:rest-api` | Ktor routes, DTO mapping, runtime profiles, OpenAPI resource | Tool-specific execution logic |
| `clients/run21ctl` | External REST client and CLI UX | In-process Kotlin runtime access |
| `tests/system` | Black-box verification | Production behavior |

## Public Boundaries

The recommended application-level entry points are:

- `ToolCalls.execute(...)` for a structured call;
- `ToolCalls.dispatch(...)` for raw input;
- `ToolExecutionService` for callers that already hold `DispatchInput`;
- REST endpoints for external processes;
- `run21ctl` as an external CLI client.

`CoreToolRuntime` and runtime factories assemble these contracts; they do not define an alternative execution model.

## Registration

Tool registration is explicit and code-based. A tool module provides descriptors and executors through `CoreToolRegistration`; runtimes compose registrations and dispatcher profiles.

The current formula registration is owned by `FormulaToolWiring`. Runtime modules should consume it rather than rebuild descriptor, implementation registry, executor, and validator wiring.

## Deployment Profiles

Current profiles are in-process and use in-memory registries:

- formula-only REST profile: structured calls through `PassThroughDispatcher`;
- Ollama REST profile: structured pass-through plus raw-input `LlmDispatcher`;
- local runtime: embedded formula execution without HTTP.

The core remains deployment-independent. Persistent registries, authentication, workers, or distributed execution belong in runtime modules added later.

## Quality Attributes

- **Simplicity:** prefer explicit small interfaces and one pipeline.
- **Testability:** core and tool behavior must be deterministic without external services.
- **Auditability:** results preserve actor, source, trace, tool, implementation, version, correlation, and timing facts.
- **Extensibility:** new tools and dispatchers use existing contracts before new abstractions are introduced.
- **Safety:** untrusted raw or model-generated input is validated before tool work.

## Documentation Ownership

Architecture details live only in this directory. User commands live in [Usage](../USAGE.md), validation commands in [Testing](../TESTING.md), and future improvements in the [Roadmap](../../ROADMAP.md).
