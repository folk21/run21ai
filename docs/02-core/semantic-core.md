# Semantic Core

## Purpose

The semantic core defines Run21AI as a dispatcher-driven tool execution engine.

Central question:

> How can input pass through a dispatcher chain, become a structured tool request, reach the correct tool executor, and return a reproducible, auditable result?

## Central Execution Model

```text
Input
  -> DispatcherChain
  -> ToolRequest
  -> ToolExecutor
  -> ToolImplementation
  -> ToolResult
```

## Core Scope

The core defines:

- actors;
- inputs;
- dispatchers;
- dispatcher chains;
- tool requests;
- tool descriptors;
- tool executors;
- tool implementation descriptors;
- tool results;
- registry contracts;
- execution errors;
- status models;
- audit metadata;
- unsupported defaults.

The core is abstract. It defines interfaces and contracts and provides safe unsupported defaults.

## Core Concepts

### Actor

An actor is a human, agent, or system identity that submits input or receives results.

### Dispatcher

A dispatcher analyzes, validates, enriches, rejects, passes, or normalizes input.

### Dispatcher Chain

A dispatcher chain is an ordered set of dispatchers. It produces a `ToolRequest` or a structured dispatch failure.

### Tool

A tool is a named executable capability with an input/output contract.

Examples:

- formula-oriented tool;
- document transformation tool;
- lookup tool;
- workflow action tool;
- spreadsheet-style tool;
- external service tool.

### Tool Request

A tool request is the normalized structured input for tool execution.

It contains:

- actor reference;
- execution source;
- target tool;
- input payload;
- optional version metadata;
- correlation metadata.

### Tool Executor

A tool executor validates a `ToolRequest`, resolves the implementation, invokes it, and shapes the `ToolResult`.

The core defines executor interfaces. Concrete executors are supplied by runtime modules.

### Tool Implementation

A tool implementation performs the real work behind a tool.

Examples:

- expression evaluator;
- JVM class;
- spreadsheet engine;
- HTTP adapter;
- local service.

### Tool Result

A tool result is the normalized output of execution.

It contains:

- status;
- output payload;
- structured error;
- tool metadata;
- implementation metadata;
- dispatcher trace;
- audit metadata.

### Unsupported Default

An unsupported default returns a structured failure when a required executor, implementation, or dispatcher is not registered.
