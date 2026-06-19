# Core Documentation Map

The core documentation defines the abstract execution model.

## Reading Order

1. [Semantic Core](semantic-core.md)
2. [Execution Model](execution-model.md)
3. [Dispatcher Chain](dispatcher-chain.md)
4. [Tool Contracts](tool-contracts.md)
5. [Core Module Contract](core-module-contract.md)
6. [Capability Status](capability-status.md)
7. [Status Model](status-model.md)

## Core Scope

The core defines interfaces and contracts for:

- dispatching input;
- producing a `ToolRequest`;
- resolving a `ToolExecutor`;
- invoking a `ToolImplementation`;
- returning a `ToolResult`;
- recording audit metadata.

The core contains almost no concrete execution implementation.
