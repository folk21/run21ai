# Deployment View

## Local Development

```text
CLI or HTTP client
  -> runtime application
  -> local storage or in-memory registry
  -> optional local LLM runtime
```

## Shared Runtime

```text
CLI / HTTP clients
  -> backend runtime
  -> persistent storage
  -> registered tool implementations
  -> optional local or external LLM provider
```

The core module is deployment-independent.
