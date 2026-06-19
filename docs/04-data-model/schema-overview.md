# Schema Overview

## Conceptual Entities

### Actor

Represents a human, software agent, or system actor.

### Tool Definition

Represents a named executable capability.

### Tool Implementation

Represents a concrete runtime implementation behind a tool.

### Tool Definition Version

Represents version metadata for tools that use versioned definitions.

### Tool Execution

Represents one execution attempt.

Fields include:

- actor reference;
- execution source;
- dispatcher trace;
- target tool snapshot;
- implementation snapshot;
- input payload;
- output payload;
- structured error;
- status;
- timestamps;
- correlation identifiers.

### Dispatch Trace

Represents dispatcher-chain steps used to produce a tool request.

## Initial Table Candidates

- `app_actor`
- `tool_definition`
- `tool_implementation`
- `tool_definition_version`
- `tool_execution`
- `dispatch_trace`

Concrete SQL migrations are introduced with runtime implementation.
