# Usage

This document owns supported invocation and runtime-configuration examples. Architecture and contract semantics are documented separately in [Architecture](architecture/ARCHITECTURE.md).

## REST API

Start the runtime:

```bash
./gradlew :runtime:rest-api:run
```

The default base URL is `http://127.0.0.1:8080`.

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/health` | Process liveness |
| `GET` | `/ready` | Runtime readiness |
| `GET` | `/docs` | Swagger UI |
| `GET` | `/openapi.yaml` | Machine-readable HTTP contract |
| `GET` | `/v1/tools` | List registered tool descriptors |
| `GET` | `/v1/tools/{code}` | Read one tool descriptor |
| `POST` | `/v1/tool-requests/resolve` | Run dispatch only and return a prepared request or rejection |
| `POST` | `/v1/executions` | Dispatch and execute the selected tool |

The source of truth for request and response shapes is [`runtime/rest-api/src/main/resources/openapi.yaml`](../runtime/rest-api/src/main/resources/openapi.yaml).

### Readiness

`GET /ready` validates the current registry assembly and configured runtime dependencies. A ready formula-only profile returns counts and component results:

```json
{
  "status": "ready",
  "tools": 1,
  "executors": 1,
  "implementations": 1,
  "checks": [
    {
      "name": "registry-assembly",
      "status": "ready",
      "details": {
        "tools": 1,
        "executors": 1,
        "implementations": 1,
        "defaultImplementations": {
          "formula.evaluate": "formula-evalex-v1"
        }
      }
    }
  ]
}
```

The Ollama profile adds an `ollama` check. It returns `not_ready` and HTTP `503` when Ollama is unavailable, the check times out, the response is invalid, or the configured model is not installed. Readiness details use stable reason values and do not expose provider response bodies or exception messages.

### Structured Execution

```bash
curl -sS -X POST http://127.0.0.1:8080/v1/executions \
  -H 'Content-Type: application/json' \
  -d '{
    "type": "structured",
    "toolCode": "formula.evaluate",
    "implementationCode": "formula-evalex-v1",
    "input": {
      "expression": "a + b",
      "variables": {"a": 10, "b": 11}
    },
    "actor": {
      "id": "usage-user",
      "type": "human",
      "displayName": "Usage User"
    },
    "source": {"type": "api", "name": "curl"},
    "correlation": {"correlationId": "usage-structured-001"}
  }'
```

The successful result contains `status`, `outputPayload`, `metadata`, and `auditMetadata`. The dispatcher trace records `pass-through` for this profile.

### Resolve Without Executing

Use resolution when a client must inspect, approve, or persist the canonical request before tool work starts:

```bash
curl -sS -X POST http://127.0.0.1:8080/v1/tool-requests/resolve \
  -H 'Content-Type: application/json' \
  -d '{
    "type": "structured",
    "toolCode": "formula.evaluate",
    "implementationCode": "formula-evalex-v1",
    "input": {
      "expression": "a + b",
      "variables": {"a": 10, "b": 11}
    },
    "actor": {"id": "usage-user", "type": "human"},
    "source": {"type": "api", "name": "curl"}
  }'
```

`/v1/tool-requests/resolve` never invokes a `ToolImplementation`.

### Prompt Dispatch With Ollama

Start the Ollama profile as described in [Installation](INSTALLATION.md), then submit raw input:

```bash
curl -sS -X POST http://127.0.0.1:8080/v1/tool-requests/resolve \
  -H 'Content-Type: application/json' \
  -d '{
    "type": "prompt",
    "prompt": "Prepare a formula.evaluate request for a + b with a = 10 and b = 11.",
    "actor": {"id": "usage-user", "type": "human"},
    "source": {"type": "api", "name": "curl"}
  }'
```

In the default formula-only profile, prompt input is rejected because only the pass-through dispatcher is registered.

## Kotlin API

Use `ToolCalls.execute(...)` when the caller already knows the tool and input payload:

```kotlin
val result = ToolCalls.execute(service) {
    tool(FormulaTool.TOOL_CODE)
    implementation(FormulaTool.IMPLEMENTATION_CODE)
    actor("user-1", ActorType.HUMAN, "User")
    source(ExecutionSourceType.TEST, "usage-example")
    input(FormulaTool.Input.EXPRESSION, "a + b")
    input(
        FormulaTool.Input.VARIABLES,
        mapOf("a" to BigDecimal("10"), "b" to BigDecimal("11")),
    )
}
```

Use `ToolCalls.dispatch(...)` when a configured dispatcher chain must prepare the request:

```kotlin
val result = ToolCalls.dispatch(service) {
    prompt("Prepare a formula evaluation request for a + b where a = 10 and b = 11.")
    actor("user-1", ActorType.HUMAN, "User")
    source(ExecutionSourceType.TEST, "usage-example")
}
```

Both calls enter the configured `DispatcherChain`. See [Execution Contracts](architecture/EXECUTION.md) for the exact responsibilities.

## Formula Tool

Current identifiers:

```text
Tool code:           formula.evaluate
Implementation code: formula-evalex-v1
```

Input fields:

| Field | Required | Meaning |
|---|---|---|
| `expression` | yes | EvalEx-compatible expression |
| `variables` | no | Named values used by the expression |
| `scale` | no | Non-negative decimal scale |
| `roundingMode` | no | `java.math.RoundingMode` value; `HALF_UP` is used when `scale` is set and this field is omitted |

The implementation returns the evaluated value plus normalized expression, variables, scale, and rounding metadata.

## Rust CLI

The CLI is independent from the Gradle build.

```bash
cargo run --manifest-path clients/run21ctl/Cargo.toml -- health
cargo run --manifest-path clients/run21ctl/Cargo.toml -- ready
cargo run --manifest-path clients/run21ctl/Cargo.toml -- tools
cargo run --manifest-path clients/run21ctl/Cargo.toml -- tool formula.evaluate
```

Execute a structured request:

```bash
cargo run --manifest-path clients/run21ctl/Cargo.toml -- execute \
  --tool-code formula.evaluate \
  --implementation-code formula-evalex-v1 \
  --input-json '{"expression":"a + b","variables":{"a":10,"b":11}}' \
  --actor-id usage-user \
  --source-name run21ctl \
  --correlation-id usage-cli-001
```

Resolve prompt input:

```bash
cargo run --manifest-path clients/run21ctl/Cargo.toml -- resolve \
  --prompt "Prepare a formula.evaluate request for 10 + 11." \
  --actor-id usage-user
```

Override the backend URL with `--base-url` or `RUN21_BASE_URL`.

## Result Semantics

- `succeeded`: tool execution completed and `outputPayload` is present.
- `failed`: a tool path was selected but execution could not complete.
- `rejected`: dispatch stopped before normal tool execution.
- Every final `ToolResult` contains audit metadata.

Stable core error codes are documented in [Execution Contracts](architecture/EXECUTION.md).
