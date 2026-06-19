# REQ-DISP-001: Dispatcher chain

Status: planned

## Goal

The system must support ordered dispatcher chains that convert input into a structured `ToolRequest` or a structured dispatch failure.

## Relationship to Core

Dispatcher chains are defined by `REQ-CORE-001` and used by `REQ-EXEC-001`.

## Functional Requirements

- A dispatcher chain must process dispatchers in configured order.
- A dispatcher may accept, enrich, reject, or pass input.
- The chain must stop when a dispatcher accepts or rejects input.
- The chain must continue when a dispatcher passes input.
- The chain must validate the final `ToolRequest` before execution.
- The chain must record dispatch trace metadata.
- A pass-through dispatcher must be available for structured direct requests.

## Acceptance Criteria

- Given the first dispatcher cannot handle input
  When another dispatcher can handle it
  Then the chain continues and produces a `ToolRequest`.

- Given dispatcher output is invalid
  When execution is attempted
  Then the system rejects the output before tool execution.

- Given dispatcher output is valid
  When execution proceeds
  Then the common tool execution path is used.

## Test Cases

- `TEST-DISP-001`: Dispatcher chain invokes dispatchers in order.
- `TEST-DISP-002`: Dispatcher chain stops after accepted output.
- `TEST-DISP-003`: Dispatcher chain stops after rejected output.
- `TEST-DISP-004`: Invalid dispatcher output is rejected.
- `TEST-DISP-005`: Valid dispatcher output produces a `ToolRequest`.
