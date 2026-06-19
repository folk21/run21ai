# Quality Attributes

## Simplicity

The core keeps the execution model small and terminology consistent.

## Testability

Core contracts must be testable without runtime frameworks.

## Extensibility

New dispatchers, executors, and implementations must be introduced through contracts and registries.

## Auditability

Every tool result must contain enough metadata to explain how execution was routed and performed.

## Determinism

Tool execution must be deterministic when the underlying implementation is deterministic. LLM dispatchers must not perform tool work.
