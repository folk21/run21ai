# Tool Contracts

## Purpose

Tool contracts define executable capabilities independently from concrete implementations.

## Tool Descriptor

A tool descriptor defines:

- tool code;
- name;
- description;
- input schema;
- output schema;
- status.

## Tool Request

A tool request defines:

- target tool code;
- input payload;
- actor metadata;
- execution source;
- dispatcher trace;
- optional version metadata;
- correlation metadata.

## Tool Executor

A tool executor validates the request, resolves the implementation, invokes it, and returns a tool result.

Contract shape:

```kotlin
interface ToolExecutor {
    val toolCode: ToolCode
    suspend fun execute(request: ToolRequest): ToolResult
}
```

## Tool Implementation Descriptor

A tool implementation descriptor defines:

- implementation code;
- implementation type;
- version;
- status;
- runtime metadata.

## Unsupported Tool Executor

Default unsupported executors must return structured failure results.

Error code:

```text
UNSUPPORTED_TOOL
```

## Contract Tests

- Registered tool is resolved by code.
- Unknown tool returns `UNKNOWN_TOOL`.
- Missing executor returns `UNSUPPORTED_TOOL`.
- Tool result always includes status and audit metadata.
