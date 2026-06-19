# Runtime View

## Structured Tool Execution

```text
Client
  -> Structured input
  -> PassThroughDispatcher
  -> ValidationDispatcher
  -> ToolRequest
  -> ToolExecutor
  -> ToolImplementation
  -> ToolResult
```

## LLM-Assisted Tool Execution

```text
Client
  -> Unstructured input
  -> RuleBasedDispatcher
  -> LlmDispatcher
  -> ValidationDispatcher
  -> ToolRequest
  -> ToolExecutor
  -> ToolImplementation
  -> ToolResult
```

## Unsupported Tool Runtime

```text
ToolRequest
  -> ToolExecutorRegistry
  -> UnsupportedToolExecutor
  -> ToolResult(status = failed, error = UNSUPPORTED_TOOL)
```
