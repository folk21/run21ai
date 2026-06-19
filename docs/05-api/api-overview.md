# API Overview

The API exposes tool execution through the dispatcher chain.

## Main Operations

```text
POST /executions
  submit structured or unstructured input

GET /executions/{id}
  read execution result and audit metadata

GET /tools
  list registered tool descriptors

GET /tools/{code}
  read one tool descriptor
```

## Execution Request

The request contains:

- input type;
- target tool when known;
- input payload;
- actor metadata;
- execution source;
- correlation metadata.

## Execution Response

The response contains:

- execution id;
- status;
- output or error;
- dispatcher trace;
- audit metadata.
