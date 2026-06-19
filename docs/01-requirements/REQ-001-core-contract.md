# REQ-CORE-001: Abstract execution core

Status: planned

## Goal

The system must define an abstract core that describes the structure of the dispatcher-driven tool execution engine.

The core defines interfaces, models, contracts, statuses, errors, registries, and audit metadata. It contains almost no concrete execution implementation.

## Relationship to Other Requirements

`REQ-CORE-001` defines the common vocabulary and contracts used by:

- `REQ-EXEC-001`: tool execution through a dispatcher chain;
- `REQ-DISP-001`: dispatcher chain behavior;
- `REQ-AUDIT-001`: versioning and audit metadata.

## Functional Requirements

- The core must define actors, inputs, dispatchers, dispatcher chains, tool requests, tools, tool executors, tool implementations, tool results, registries, errors, statuses, and audit metadata.
- The core must define execution as `Input -> DispatcherChain -> ToolRequest -> ToolExecutor -> ToolImplementation -> ToolResult`.
- The core must define unsupported defaults for missing tools, executors, implementations, and dispatchers.
- The core must remain independent from runtime frameworks, persistence frameworks, LLM clients, expression engines, and CLI libraries.

## Acceptance Criteria

- Given the core is built
  When its dependency graph is inspected
  Then it contains no runtime framework dependency.

- Given structured input is submitted
  When the dispatcher chain uses a pass-through dispatcher
  Then it produces a valid `ToolRequest`.

- Given unstructured input is submitted
  When the dispatcher chain accepts it
  Then it produces the same `ToolRequest` model.

- Given a target tool has no registered executor
  When execution is requested
  Then the core returns a structured unsupported tool error.

## Test Cases

- `TEST-CORE-001`: Core models compile without runtime framework dependencies.
- `TEST-CORE-002`: Pass-through dispatcher produces a valid `ToolRequest`.
- `TEST-CORE-003`: Dispatcher output and direct structured input use the same `ToolRequest` model.
- `TEST-CORE-004`: Unsupported tool executor returns a structured failure.
- `TEST-CORE-005`: `ToolResult` always contains status and audit metadata.
