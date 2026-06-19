# Requirements Map

This folder defines system requirements and their relationships.

## Requirement Chain

```text
REQ-CORE-001
  defines the abstract execution core

REQ-EXEC-001
  defines tool execution through a dispatcher chain

REQ-DISP-001
  defines dispatcher chain behavior

REQ-AUDIT-001
  defines versioning and audit metadata
```

## Reading Order

1. [Requirement Format](requirement-format.md)
2. [REQ-CORE-001: Abstract execution core](REQ-001-core-contract.md)
3. [REQ-EXEC-001: Tool execution through dispatcher chain](REQ-002-tool-execution.md)
4. [REQ-DISP-001: Dispatcher chain](REQ-003-dispatcher-chain.md)
5. [REQ-AUDIT-001: Versioning and audit metadata](REQ-004-versioning-audit.md)

## Core Relationship

The core defines the abstract structure of the system:

- dispatcher chain;
- tool request;
- tool executor;
- tool implementation;
- tool result;
- status, error, and audit contracts.

Runtime modules provide concrete dispatchers, executors, implementations, APIs, persistence, and clients.
