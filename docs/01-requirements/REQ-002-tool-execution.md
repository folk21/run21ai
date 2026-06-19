# REQ-EXEC-001: Tool execution through dispatcher chain

Status: planned

## Goal

The system must execute tool requests through a registered dispatcher chain.

Direct structured execution is represented by a pass-through dispatcher chain. LLM, rule-based, validation, and custom dispatchers use the same execution path.

## Relationship to Core

Tool execution is built on `REQ-CORE-001`.

```text
Input -> DispatcherChain -> ToolRequest -> ToolExecutor -> ToolImplementation -> ToolResult
```

## Functional Requirements

- A caller must be able to submit structured input that targets a tool.
- A caller may submit unstructured input if a dispatcher chain can normalize it.
- Every execution request must pass through a dispatcher chain.
- A pass-through dispatcher must support direct structured calls.
- The dispatcher chain must return a `ToolRequest` or a structured dispatch error.
- The tool executor must resolve the target tool implementation through the core registry contract.
- Tool execution must produce a `ToolResult` with output or error and audit metadata.
- Tool execution must not invoke an LLM unless an LLM dispatcher is registered in the chain.

## Acceptance Criteria

- Given structured input targets a registered tool
  When the pass-through dispatcher accepts it
  Then the system executes the tool through the registered executor.

- Given unstructured input is submitted
  When a dispatcher normalizes it into a valid `ToolRequest`
  Then the system executes the tool through the same executor path.

- Given execution finishes
  When the result is returned
  Then the result contains status, output or error, and audit metadata.

- Given no executor is registered for the target tool
  When execution is requested
  Then a structured unsupported tool error is returned.

## Test Cases

- `TEST-EXEC-001`: Structured tool request uses pass-through dispatching.
- `TEST-EXEC-002`: Dispatcher-produced request uses the same tool executor path.
- `TEST-EXEC-003`: Execution returns structured audit metadata.
- `TEST-EXEC-004`: Missing executor returns structured unsupported tool error.
