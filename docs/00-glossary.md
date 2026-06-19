# Glossary

## Actor

A human, software agent, or system identity that submits input or receives results.

## Input

Raw or structured data submitted to the system.

## Dispatcher

A component that analyzes, validates, enriches, normalizes, routes, rejects, or passes input.

## Dispatcher Chain

An ordered sequence of dispatchers. It produces a `ToolRequest` or a structured dispatch failure.

## Pass-through Dispatcher

A dispatcher that accepts already structured input and converts it into a `ToolRequest` without semantic analysis.

## LLM Dispatcher

A dispatcher that uses a local or external LLM to convert unstructured input into a structured `ToolRequest`.

## Tool

A named executable capability with an input/output contract.

## Tool Request

A normalized structured request targeting one tool.

## Tool Executor

A component that validates a `ToolRequest`, resolves the target implementation, invokes it, and returns a `ToolResult`.

## Tool Implementation

A concrete runtime mechanism behind a tool, such as an expression engine, JVM class, spreadsheet engine, HTTP adapter, or local service.

## Tool Result

A normalized execution result containing status, output or error, and audit metadata.

## Audit Metadata

Metadata describing who requested execution, what was executed, how it was routed, which implementation was used, and what result was produced.

## Capability Status

The implementation progress of a capability. Allowed statuses are `planned`, `in_progress`, and `done`.

## Formula Tool

A tool type that evaluates formula-like definitions with structured variables. It is an example of a tool, not a separate execution model.
