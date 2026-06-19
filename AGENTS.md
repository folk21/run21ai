# AGENTS.md

This file defines instructions for AI coding agents working on Run21AI.

## Project Intent

Run21AI is a lightweight dispatcher-driven tool execution engine.

The system routes input through a dispatcher chain, converts it into a structured `ToolRequest`, executes the target tool through a registered `ToolExecutor` and `ToolImplementation`, and returns an auditable `ToolResult`.

Central model:

```text
Input -> DispatcherChain -> ToolRequest -> ToolExecutor -> ToolImplementation -> ToolResult
```

## Core Rule

The `core` module is the framework-independent abstract execution kernel.

It defines:

* models;
* interfaces;
* dispatcher contracts;
* dispatcher chain contracts;
* tool request/result contracts;
* tool executor contracts;
* tool implementation contracts;
* registry contracts;
* status and error models;
* audit metadata;
* unsupported defaults;
* convenience APIs such as `ToolCalls`.

The `core` module must not depend on runtime frameworks, persistence frameworks, CLI libraries, LLM clients, expression engines, or external service SDKs.

## Execution Rule

All execution must pass through a `DispatcherChain`.

Structured execution uses:

```text
ToolCalls.execute
  -> StructuredDispatchInput
  -> PassThroughDispatcher
  -> ToolRequest
  -> ToolExecutor
  -> ToolImplementation
  -> ToolResult
```

Raw or prompt-like execution uses:

```text
ToolCalls.dispatch
  -> RawDispatchInput
  -> DispatcherChain
  -> ToolRequest
  -> ToolExecutor
  -> ToolImplementation
  -> ToolResult
```

Direct execution is not a separate execution model. It is structured input handled by a pass-through dispatcher.

## LLM Rule

An LLM may be used only as a dispatcher inside the dispatcher chain.

An LLM dispatcher may:

* analyze raw input;
* classify intent;
* select a target tool;
* prepare structured arguments;
* enrich metadata;
* reject ambiguous or unsafe input.

An LLM dispatcher must not:

* perform real tool work;
* calculate final business results;
* bypass `ToolExecutor`;
* bypass audit metadata;
* directly call external systems unless that behavior is implemented as a tool.

Mock LLM dispatchers are allowed in tests and examples.

## Terminology Rule

Use these terms consistently:

* `Dispatcher`
* `DispatcherChain`
* `DispatchInput`
* `StructuredDispatchInput`
* `RawDispatchInput`
* `ToolRequest`
* `ToolExecutor`
* `ToolImplementation`
* `ToolExecutionOutput`
* `ToolResult`
* `ToolError`
* `AuditMetadata`
* `ToolCalls`

Do not introduce parallel terms for the same concept.

Do not treat “calculation” as a separate execution path. Formula-like or calculation-like behavior is just one possible tool family.

## ToolCalls Rule

`ToolCalls` is the preferred high-level API for examples and most contract tests.

Use `ToolCalls.execute(...)` for structured tool calls.

Use `ToolCalls.dispatch(...)` for raw input that must pass through a dispatcher chain.

Low-level tests may bypass `ToolCalls` only when they directly test:

* dispatcher behavior;
* dispatcher chain behavior;
* registries;
* validation;
* unsupported defaults;
* builders.

## Scope Rule

Generate small slices.

For each slice, include:

* model/interface changes when needed;
* implementation changes when needed;
* contract tests;
* README updates when public usage changes;
* documentation updates when terminology or behavior changes.

Do not generate unrelated runtime framework code inside the `core` module.

## Testing Rule

Core tests must validate contracts, status behavior, unsupported defaults, dispatcher-chain behavior, audit metadata, and recommended call APIs.

Tests must remain deterministic.

Do not introduce external services into core tests.

Runtime implementations must be tested in their own modules.

## Documentation Rule

All documentation, code comments, KDoc, configuration comments, and generated text must be in English.

Documentation changes must keep requirements, glossary, architecture, testing, generation rules, and README examples aligned.

## Style Rule

Use idiomatic Kotlin.

Prefer:

* immutable data classes;
* explicit small interfaces;
* sealed classes or enums for controlled states;
* stable public API names;
* readable tests;
* concise KDoc for public classes and important public functions.

Avoid:

* unnecessary file-level comments;
* noisy comments that restate the code;
* reflection unless explicitly required;
* global mutable state;
* framework annotations in `core`;
* over-engineered abstractions.
