# Architecture Overview

## Summary

Run21AI is a dispatcher-driven tool execution engine.

```text
Input -> DispatcherChain -> ToolRequest -> ToolExecutor -> ToolImplementation -> ToolResult
```

## Architectural Rules

- The core is abstract and framework-independent.
- All execution passes through a dispatcher chain.
- Direct structured calls use a pass-through dispatcher.
- LLM-based calls use an LLM dispatcher inside the chain.
- Tool execution is performed by registered tool executors and implementations.
- Unsupported capabilities fail with structured errors.
- Every result includes audit metadata.

## Main Building Blocks

### Core

Defines contracts, models, registries, status types, error types, audit metadata, and unsupported defaults.

### Runtime

Registers dispatchers, tool executors, tool implementations, persistence, APIs, and client-facing workflows.

### Tool Implementations

Perform real work behind tool contracts.

### Clients

Submit structured or unstructured input through API or CLI.

## Dependency Direction

Runtime modules depend on core contracts. The core does not depend on runtime modules.
