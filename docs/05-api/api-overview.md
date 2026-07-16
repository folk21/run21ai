# API Overview

The REST API exposes dispatcher-chain request resolution and tool execution.

Start the local runtime from the project root:

```bash
./gradlew :runtime:rest-api:run
```

The examples below assume the server is listening on `http://127.0.0.1:8080`.

## Main Operations

```text
GET /health
  check that the HTTP process is alive

GET /ready
  check that the runtime has registered tools

GET /docs
  open interactive Swagger UI

GET /openapi.yaml
  read the OpenAPI contract

GET /v1/tools
  list registered tool descriptors

GET /v1/tools/{code}
  read one tool descriptor

POST /v1/tool-requests/resolve
  run the dispatcher chain and return a prepared ToolRequest or a dispatch error

POST /v1/executions
  run the dispatcher chain and execute the selected tool through ToolExecutor
```

## Execution Request

The request contains:

- input type;
- target tool when known;
- input payload;
- actor metadata;
- execution source;
- correlation metadata.

### Structured Formula Execution

Structured requests already name the tool and provide its input payload. They still pass through the dispatcher chain; in the default formula runtime, the pass-through dispatcher accepts the request.

```bash
curl -sS -X POST http://127.0.0.1:8080/v1/executions \
  -H 'Content-Type: application/json' \
  -d '{
    "type": "structured",
    "toolCode": "formula.evaluate",
    "implementationCode": "formula-evalex-v1",
    "input": {
      "expression": "a + b",
      "variables": {
        "a": 10,
        "b": 11
      }
    },
    "actor": {
      "id": "api-doc-user",
      "type": "human",
      "displayName": "API Documentation User"
    },
    "source": {
      "type": "api",
      "name": "curl"
    },
    "correlation": {
      "correlationId": "api-doc-structured-formula"
    }
  }'
```

Successful responses include the tool output and audit metadata. This example is trimmed to the fields most clients normally inspect; schema objects are shortened for readability:

```json
{
  "status": "succeeded",
  "outputPayload": {
    "value": 21,
    "valueType": "number",
    "expression": "a + b",
    "variables": {
      "a": 10,
      "b": 11
    },
    "scale": null,
    "roundingMode": null
  },
  "error": null,
  "auditMetadata": {
    "actor": {
      "id": "api-doc-user",
      "type": "human",
      "displayName": "API Documentation User"
    },
    "source": {
      "type": "api",
      "name": "curl",
      "metadata": {}
    },
    "dispatcherTrace": [
      {
        "dispatcherCode": "pass-through",
        "outcome": "accepted",
        "confidence": 1.0,
        "inputSummary": "Structured tool request.",
        "outputSummary": "Accepted structured tool request.",
        "correlationId": "api-doc-structured-formula",
        "metadata": {}
      }
    ],
    "tool": {
      "code": "formula.evaluate",
      "name": "Formula Evaluation",
      "status": "done",
      "inputSchema": {},
      "outputSchema": {}
    },
    "implementation": {
      "implementationCode": "formula-evalex-v1",
      "toolCode": "formula.evaluate",
      "type": "formula",
      "version": "1.0.0",
      "status": "done",
      "runtimeMetadata": {
        "engine": "EvalEx",
        "deterministic": true,
        "numericType": "BigDecimal",
        "defaultRoundingMode": "HALF_UP"
      }
    },
    "correlation": {
      "correlationId": "api-doc-structured-formula"
    },
    "startedAt": "2026-07-14T10:15:30Z",
    "completedAt": "2026-07-14T10:15:30Z",
    "metadata": {}
  },
  "metadata": {
    "engine": "EvalEx",
    "implementation": "formula-evalex-v1",
    "implementationVersion": "1.0.0"
  }
}
```

Schema fields, generated identifiers, and timestamps depend on the registered tool descriptor and runtime clock. The important contract is that the final result is produced by the tool implementation, and the dispatcher trace records how the request was accepted.

### Resolve Without Executing

Use request resolution when a client needs to inspect the canonical `ToolRequest` produced by the dispatcher chain before executing any tool work.

```bash
curl -sS -X POST http://127.0.0.1:8080/v1/tool-requests/resolve \
  -H 'Content-Type: application/json' \
  -d '{
    "type": "structured",
    "toolCode": "formula.evaluate",
    "implementationCode": "formula-evalex-v1",
    "input": {
      "expression": "a + b",
      "variables": {
        "a": 10,
        "b": 11
      }
    },
    "actor": {
      "id": "api-doc-user",
      "type": "human"
    },
    "source": {
      "type": "api",
      "name": "curl"
    },
    "correlation": {
      "correlationId": "api-doc-resolve-formula"
    }
  }'
```

Example response:

```json
{
  "status": "accepted",
  "request": {
    "targetTool": "formula.evaluate",
    "targetImplementation": "formula-evalex-v1",
    "inputPayload": {
      "expression": "a + b",
      "variables": {
        "a": 10,
        "b": 11
      }
    },
    "actor": {
      "id": "api-doc-user",
      "type": "human"
    },
    "source": {
      "type": "api",
      "name": "curl",
      "metadata": {}
    },
    "correlation": {
      "correlationId": "api-doc-resolve-formula"
    },
    "metadata": {}
  },
  "error": null,
  "trace": [
    {
      "dispatcherCode": "pass-through",
      "outcome": "accepted",
      "confidence": 1.0,
      "inputSummary": "Structured tool request.",
      "outputSummary": "Accepted structured tool request.",
      "correlationId": "api-doc-resolve-formula",
      "metadata": {}
    }
  ]
}
```

### Prompt Resolution

Prompt-like requests use `type: "prompt"` and must be handled by a raw-input dispatcher in the configured dispatcher chain. In the default formula-only runtime, no raw dispatcher is registered, so prompt resolution is rejected in a structured way.

```bash
curl -sS -X POST http://127.0.0.1:8080/v1/tool-requests/resolve \
  -H 'Content-Type: application/json' \
  -d '{
    "type": "prompt",
    "prompt": "Evaluate formula a + b with a = 10 and b = 11 using formula.evaluate.",
    "actor": {
      "id": "api-doc-user",
      "type": "human"
    },
    "source": {
      "type": "api",
      "name": "curl"
    },
    "correlation": {
      "correlationId": "api-doc-prompt-formula"
    }
  }'
```

Default formula-only response:

```json
{
  "status": "rejected",
  "request": null,
  "error": {
    "code": "NO_DISPATCHER_ACCEPTED",
    "message": "Dispatcher chain did not produce a tool request.",
    "details": {}
  },
  "trace": [
    {
      "dispatcherCode": "pass-through",
      "outcome": "passed",
      "confidence": 1.0,
      "inputSummary": "Non-structured dispatch input.",
      "outputSummary": "Input was not handled by pass-through dispatcher.",
      "correlationId": "api-doc-prompt-formula",
      "metadata": {}
    }
  ]
}
```

When the REST runtime is started with a profile that registers an LLM or other raw-input dispatcher, the same endpoint can return `status: "accepted"` with a prepared `ToolRequest`. The dispatcher prepares the request only; it does not calculate the formula result.

### Tool Discovery

List registered tools:

```bash
curl -sS http://127.0.0.1:8080/v1/tools
```

Read the formula tool descriptor:

```bash
curl -sS http://127.0.0.1:8080/v1/tools/formula.evaluate
```

## Execution Response

The response contains:

- status;
- output or error;
- dispatcher trace;
- audit metadata.
