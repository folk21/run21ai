# Tool Contract

## Purpose

A tool is a stable executable capability.

## Tool Descriptor

A descriptor defines:

- code;
- name;
- description;
- input schema;
- output schema;
- status.

## Tool Executor

A tool executor validates a request, resolves an implementation, delegates work, and returns a result.

## Tool Implementation

A tool implementation performs real work.

## Unsupported Behavior

If a tool has no registered executor or implementation, execution returns a structured error.

Stable errors:

- `UNKNOWN_TOOL`
- `UNSUPPORTED_TOOL`
- `IMPLEMENTATION_NOT_REGISTERED`
- `INVALID_TOOL_REQUEST`
