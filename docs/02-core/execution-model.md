# Execution Model

## Purpose

The execution model describes how input reaches a tool.

All execution flows use the same normalized model:

```text
Input -> DispatcherChain -> ToolRequest -> ToolExecutor -> ToolImplementation -> ToolResult
```

`CoreToolRuntime` is the root-level facade for callers that want one visible core entry point. It
delegates to a configured `ToolExecutionService` and does not introduce a separate execution path.
For simple in-memory composition, `CoreToolRuntime.inMemory(...)` creates the standard
dispatcher-chain, registry, and execution-service wiring. Runtimes that also need request
resolution or discovery access can use `CoreToolRuntime.inMemoryAssembly(...)` to reuse the same
dispatcher chain and registries.
Tool modules can expose a `CoreToolRegistration` so runtime modules can install the tool without
duplicating descriptor, implementation registry, executor, and validator wiring.

## Structured Direct Execution

```text
Caller
  -> CoreToolRuntime or ToolCalls
  -> Structured input
  -> PassThroughDispatcher
  -> ToolRequest
  -> ToolExecutionService
  -> ToolExecutorRegistry
  -> ToolExecutor
  -> ToolImplementation
  -> ToolResult
```

Structured direct execution is used when the caller already knows the target tool and can provide structured input.

## Dispatcher-Based Execution

```text
Caller
  -> CoreToolRuntime or ToolCalls
  -> Input
  -> DispatcherChain
  -> ToolRequest
  -> ToolExecutionService
  -> ToolExecutorRegistry
  -> ToolExecutor
  -> ToolImplementation
  -> ToolResult
```

Dispatcher-based execution is used when input needs normalization, enrichment, classification, validation, or routing.

## Tool Executor Responsibility

A tool executor is responsible for:

- request validation;
- implementation resolution;
- implementation invocation;
- result normalization;
- stable error codes;
- audit metadata preparation.

## Tool Implementation Responsibility

A tool implementation performs the real work.

Examples:

- evaluating an expression;
- running a JVM class;
- processing a spreadsheet-style template;
- calling an external HTTP service.

## Result Responsibility

Every `ToolResult` must include:

- execution status;
- output or error;
- execution source;
- actor reference;
- dispatcher trace;
- tool and implementation snapshot;
- version metadata when applicable.
