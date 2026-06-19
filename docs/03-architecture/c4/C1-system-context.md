# C1: System Context

```text
Human / Agent / Client
  -> Run21AI
  -> Tool Implementations
  -> Optional LLM Provider
  -> Optional Persistent Storage
```

Run21AI accepts structured or unstructured input, routes it through a dispatcher chain, executes a target tool, and returns an auditable result.
