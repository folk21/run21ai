# Execution Contracts

This document owns the canonical execution model and the responsibilities of core abstractions.

## Canonical Pipeline

```text
DispatchInput
  -> DispatcherChain
  -> ToolRequest
  -> ToolExecutionService
  -> ToolExecutor
  -> ToolImplementation
  -> ToolResult
```

There is no separate calculation path, direct path, or LLM execution path. Different input forms converge on the same `ToolRequest` and use the same executor, implementation, result, error, and audit contracts.

## Entry Modes

### Structured Call

```text
ToolCalls.execute
  -> StructuredDispatchInput
  -> PassThroughDispatcher
  -> ToolRequest
  -> ToolExecutor
  -> ToolImplementation
  -> ToolResult
```

The pass-through dispatcher accepts the existing request without semantic interpretation.

### Raw Call

```text
ToolCalls.dispatch
  -> RawDispatchInput
  -> configured DispatcherChain
  -> ToolRequest
  -> ToolExecutor
  -> ToolImplementation
  -> ToolResult
```

A raw-input dispatcher may parse, validate, enrich, classify, route, or reject input. It does not perform tool work.

## Core Contracts

| Contract | Responsibility |
|---|---|
| `DispatchInput` | Caller input plus actor, source, correlation, trace, and attributes |
| `Dispatcher` | Produces one dispatcher outcome for the current input |
| `DispatcherChain` | Runs dispatchers in order and returns an accepted request or structured rejection |
| `ToolRequest` | Canonical target tool, optional implementation, payload, actor, source, version, correlation, and metadata |
| `ToolExecutionService` | Coordinates dispatch, tool lookup, executor lookup, and final execution |
| `ToolExecutor` | Validates a request, resolves an implementation, invokes it, and creates the final result |
| `ToolImplementation` | Performs tool-specific work and returns `ToolExecutionOutput` |
| `ToolResult` | Final status, output or error, audit metadata, and caller-facing metadata |

## Dispatcher Outcomes

A dispatcher returns exactly one outcome:

| Outcome | Chain behavior |
|---|---|
| `accepted` | Stop and validate the produced `ToolRequest` |
| `rejected` | Stop with a structured `ToolError` |
| `enriched` | Continue with the replacement input |
| `passed` | Continue with the current input |

The first accepted or rejected outcome is terminal. If no dispatcher accepts or rejects input, the chain returns `NO_DISPATCHER_ACCEPTED`. An empty or unavailable chain returns `DISPATCHER_NOT_REGISTERED`.

Every dispatcher decision is appended to `DispatcherTrace`.

## Caller Provenance

The dispatcher chain owns caller provenance for the complete dispatch run:

- `actor`;
- `source`;
- `correlation`.

Dispatchers may enrich payloads and attributes or prepare tool routing and request metadata. They
must not replace caller provenance. `OrderedDispatcherChain` copies the original provenance into
every enriched input and into the accepted `ToolRequest` before validation, so downstream
dispatchers, execution, and audit metadata observe the caller values supplied at the chain boundary.

## Execution Sequence

After dispatch accepts a request:

1. `DefaultToolExecutionService` resolves the `ToolDescriptor` from `ToolRegistry`.
2. An unknown descriptor returns `UNKNOWN_TOOL`.
3. The service resolves a `ToolExecutor`; a missing executor is represented by `UnsupportedToolExecutor` and returns `UNSUPPORTED_TOOL`.
4. The executor validates that it owns the target tool and applies its request validator.
5. The executor resolves the requested or default `ToolImplementation`.
6. A missing implementation returns `IMPLEMENTATION_NOT_REGISTERED`.
7. The implementation performs the actual work.
8. The executor wraps output and snapshots into a `ToolResult`.

## Registries

| Registry | Stores | Selection responsibility |
|---|---|---|
| `ToolRegistry` | `ToolDescriptor` values | Declares whether a tool exists and exposes its contract |
| `ToolExecutorRegistry` | One executor per tool code | Selects the tool-level execution coordinator |
| `ToolImplementationRegistry` | Implementations grouped by tool | Selects an exact implementation or the current in-memory default |
| `DispatcherChainRegistry` | Named dispatcher-chain profiles | Optional runtime-level profile lookup |

In-memory registries are suitable for tests and current runtime profiles. Persistence-backed registries may be added outside core without changing these interfaces.

## Validation

Validation occurs at two levels:

- core validation checks invariants required by every runtime, such as tool identity, actor identity, and correlation identifiers;
- tool validation checks the concrete payload contract before implementation execution.

`ContractSchema` exposes descriptor metadata without binding core to a particular schema library. The current runtime does not automatically validate every payload against this schema; tool validators remain the enforced contract. This limitation is tracked in the [Roadmap](../ROADMAP.md).

## Status and Errors

Execution statuses:

- `accepted` — dispatcher chain produced a request;
- `running` — reserved execution state;
- `succeeded` — tool work completed;
- `failed` — a tool path was selected but execution failed;
- `rejected` — dispatch stopped before normal tool execution.

Current stable core error codes:

- `UNKNOWN_TOOL`
- `UNSUPPORTED_TOOL`
- `IMPLEMENTATION_NOT_REGISTERED`
- `DISPATCHER_NOT_REGISTERED`
- `INVALID_TOOL_REQUEST`
- `DISPATCH_REJECTED`
- `NO_DISPATCHER_ACCEPTED`
- `DISPATCHER_FAILED`
- `TOOL_EXECUTION_FAILED`
- `TIMEOUT`

## Exception Semantics

Unexpected failures are normalized at the component boundary:

- dispatcher failures become `DISPATCHER_FAILED` and stop dispatch with `rejected` status;
- tool executor or implementation failures become `TOOL_EXECUTION_FAILED` with `failed` status;
- dispatcher, executor, implementation, network, and provider timeouts become `TIMEOUT`;
- dispatcher failures are recorded as rejected trace steps when the concrete dispatcher is known;
- all normalized failures retain audit metadata;
- raw exception classes, messages, and stack traces are not included in public error details.

Cancellation is not a business failure. `CancellationException` and interruption are propagated to the caller so structured concurrency, request cancellation, and shutdown remain effective. Coroutine-style timeout cancellation is classified as `TIMEOUT` before general cancellation handling.

## Audit Metadata

Every final `ToolResult` contains `AuditMetadata` with snapshots of:

- actor;
- execution source;
- dispatcher trace;
- target tool descriptor;
- selected implementation descriptor when available;
- request version metadata when supplied;
- execution and correlation identifiers;
- start and completion timestamps;
- additional execution metadata.

Snapshots preserve the meaning of a result even if registry entries later change.

## Formula Tool

The first concrete tool is:

```text
Tool:           formula.evaluate
Implementation: formula-evalex-v1
Engine:         EvalEx
Numeric model:  BigDecimal
```

The formula module owns its descriptor, input validation, implementation, and wiring. It is an example of an ordinary tool and does not introduce formula-specific core semantics.

## Unsupported and Future States

Core provides controlled defaults for missing dispatch and execution support and normalizes unexpected component failures. The project still needs explicit default-implementation policy, registration consistency checks, execution budgets, and transport-level JSON value validation. These are implementation gaps, not alternative architectural models.
