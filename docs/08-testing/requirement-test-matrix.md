# Requirement-Test Matrix

This matrix maps documented requirements to automated tests and expected coverage.

## Coverage Levels

| Level | Meaning |
|---|---|
| Contract | Tests framework-independent core contracts and public APIs. |
| Integration | Tests a concrete module wired through the core contracts. |
| API | Tests the HTTP boundary and JSON mapping. |
| System | Tests a running service through external clients. |

## Requirement Coverage

| Requirement | Test Case | Coverage Level | Test Location | Expected Coverage |
|---|---|---|---|---|
| `REQ-CORE-001` | `TEST-CORE-001` | Contract | `core/src/test/kotlin/ai/run21/core/CoreDependencyContractTest.kt` | Core module has no runtime framework, persistence framework, LLM client, expression engine, CLI, or external SDK dependency. |
| `REQ-CORE-001` | `TEST-CORE-002` | Contract | `core/src/test/kotlin/ai/run21/core/PassThroughDispatcherContractTest.kt` | Structured input handled by the pass-through dispatcher produces a valid canonical `ToolRequest`. |
| `REQ-CORE-001` | `TEST-CORE-003` | Contract | `core/src/test/kotlin/ai/run21/core/ToolCallsContractTest.kt`, `core/src/test/kotlin/ai/run21/core/ModelInvariantContractTest.kt` | Structured input and dispatcher-produced input use the same `ToolRequest` model and validate required model invariants. |
| `REQ-CORE-001` | `TEST-CORE-004` | Contract | `core/src/test/kotlin/ai/run21/core/UnsupportedDefaultsContractTest.kt`, `core/src/test/kotlin/ai/run21/core/RegistryContractTest.kt` | Missing tools, executors, implementations, and dispatchers return structured unsupported or lookup failures instead of ad hoc errors. |
| `REQ-CORE-001` | `TEST-CORE-005` | Contract | `core/src/test/kotlin/ai/run21/core/AuditMetadataContractTest.kt`, `core/src/test/kotlin/ai/run21/core/ToolExecutionContractTest.kt` | `ToolResult` always carries status and audit metadata for success and failure paths. |
| `REQ-EXEC-001` | `TEST-EXEC-001` | Contract | `core/src/test/kotlin/ai/run21/core/ToolCallsContractTest.kt`, `core/src/test/kotlin/ai/run21/core/ToolExecutionContractTest.kt` | Structured `ToolCalls.execute(...)` input is converted to `StructuredDispatchInput`, accepted by pass-through dispatching, and executed through the configured executor. |
| `REQ-EXEC-001` | `TEST-EXEC-002` | Contract | `core/src/test/kotlin/ai/run21/core/ToolCallsContractTest.kt`, `core/src/test/kotlin/ai/run21/core/DispatcherChainContractTest.kt` | Raw `ToolCalls.dispatch(...)` input normalized by a dispatcher uses the same `ToolExecutor` path as structured execution. |
| `REQ-EXEC-001` | `TEST-EXEC-003` | Contract, Integration, API | `core/src/test/kotlin/ai/run21/core/ToolExecutionContractTest.kt`, `tools/formula/src/test/kotlin/ai/run21/tools/formula/FormulaToolIntegrationTest.kt`, `runtime/rest-api/src/test/kotlin/ai/run21/runtime/restapi/RestApiApplicationTest.kt` | Finished executions return status, output or error, dispatcher trace, actor, source, correlation, tool, and implementation audit metadata. |
| `REQ-EXEC-001` | `TEST-EXEC-004` | Contract | `core/src/test/kotlin/ai/run21/core/UnsupportedDefaultsContractTest.kt`, `core/src/test/kotlin/ai/run21/core/RegistryContractTest.kt` | Missing executor or implementation returns a structured unsupported result. |
| `REQ-DISP-001` | `TEST-DISP-001` | Contract | `core/src/test/kotlin/ai/run21/core/DispatcherChainContractTest.kt` | Dispatcher chain invokes dispatchers in configured order. |
| `REQ-DISP-001` | `TEST-DISP-002` | Contract | `core/src/test/kotlin/ai/run21/core/DispatcherChainContractTest.kt` | Dispatcher chain stops after an accepted dispatcher output. |
| `REQ-DISP-001` | `TEST-DISP-003` | Contract | `core/src/test/kotlin/ai/run21/core/DispatcherChainContractTest.kt` | Dispatcher chain stops after a rejected dispatcher output and returns a structured dispatch error. |
| `REQ-DISP-001` | `TEST-DISP-004` | Contract | `core/src/test/kotlin/ai/run21/core/DispatcherChainContractTest.kt`, `core/src/test/kotlin/ai/run21/core/ModelInvariantContractTest.kt` | Invalid dispatcher output is rejected before tool execution. |
| `REQ-DISP-001` | `TEST-DISP-005` | Contract, API | `core/src/test/kotlin/ai/run21/core/PassThroughDispatcherContractTest.kt`, `runtime/rest-api/src/test/kotlin/ai/run21/runtime/restapi/RestApiApplicationTest.kt` | Valid dispatcher output produces a `ToolRequest`; `/v1/tool-requests/resolve` returns the request without executing tool work. |
| `REQ-AUDIT-001` | `TEST-AUDIT-001` | Contract, API, System | `core/src/test/kotlin/ai/run21/core/AuditMetadataContractTest.kt`, `runtime/rest-api/src/test/kotlin/ai/run21/runtime/restapi/RestApiApplicationTest.kt`, `tests/system/tests/test_formula_execution.py` | Successful execution result identifies actor, source, dispatcher trace, tool descriptor snapshot, implementation descriptor snapshot, correlation, and timestamps. |
| `REQ-AUDIT-001` | `TEST-AUDIT-002` | Contract, Integration | `core/src/test/kotlin/ai/run21/core/AuditMetadataContractTest.kt`, `tools/formula/src/test/kotlin/ai/run21/tools/formula/FormulaToolImplementationTest.kt` | Failed execution result still carries audit metadata and a structured `ToolError`. |
| `REQ-AUDIT-001` | `TEST-AUDIT-003` | Contract | `core/src/test/kotlin/ai/run21/core/ToolCallsContractTest.kt`, `core/src/test/kotlin/ai/run21/core/AuditMetadataContractTest.kt` | Version metadata supplied on the request is preserved in audit metadata and result snapshots. |

## Runtime and Client Regression Coverage

These tests do not define new core requirements. They protect runtime behavior that implements the
requirements above.

| Area | Coverage Level | Test Location | Expected Coverage |
|---|---|---|---|
| Formula tool | Integration | `tools/formula/src/test/kotlin/ai/run21/tools/formula/FormulaToolImplementationTest.kt`, `tools/formula/src/test/kotlin/ai/run21/tools/formula/FormulaToolIntegrationTest.kt` | Formula evaluation succeeds for valid input and returns structured errors for invalid input. |
| Local runtime | Integration | `runtime/local/src/test/kotlin/ai/run21/runtime/local/LocalRun21RuntimeTest.kt` | Local runtime wires the dispatcher chain, formula tool descriptor, implementation registry, and executor registry correctly. |
| REST API | API | `runtime/rest-api/src/test/kotlin/ai/run21/runtime/restapi/RestApiApplicationTest.kt` | Health, readiness, OpenAPI, Swagger UI, tool discovery, structured execution, request resolution, and prompt rejection are exposed over HTTP. |
| LLM dispatcher contract | Contract, Integration | `core/src/test/kotlin/ai/run21/core/MockLlmDispatcherContractTest.kt`, `dispatchers/llm/src/test/kotlin/ai/run21/dispatchers/llm/LlmDispatcherTest.kt` | LLM-like dispatchers prepare `ToolRequest` objects but do not perform tool work or bypass execution. |
| Ollama dispatcher adapter | Integration | `dispatchers/llm-ollama/src/test/kotlin/ai/run21/dispatchers/llm/ollama/*.kt` | Prompt building, response parsing, response validation, client configuration, and Ollama dispatch behavior remain deterministic under tests. |
| `run21ctl` CLI | Client | `clients/run21ctl/tests/cli_contract_test.rs` | CLI parses commands and request flags into the REST request shape for health, tool discovery, structured execution, prompt resolution, and invalid JSON rejection. |
| System smoke tests | System | `tests/system/tests/*.py` | Running service supports health, readiness, tool discovery, structured formula execution, prompt resolution behavior, and CLI smoke calls. |

## Known Gaps

| Gap | Related Requirement | Suggested Test |
|---|---|---|
| Dependency checks should fail on newly introduced forbidden dependencies, not only document the expected policy. | `REQ-CORE-001` | Keep `CoreDependencyContractTest` synchronized with Gradle dependency declarations and add forbidden dependency names when new runtime modules are introduced. |
| Versioned tool definitions are represented in core metadata, but there is no separate concrete versioned tool implementation. | `REQ-AUDIT-001` | Add a small test-only versioned tool implementation and assert exact definition and implementation versions in the final `ToolResult`. |
| Prompt execution through a real raw-input dispatcher is covered by fake or adapter tests, while the default REST runtime currently documents structured rejection. | `REQ-EXEC-001`, `REQ-DISP-001` | Add a runtime profile or test fixture that registers a deterministic raw dispatcher and verifies prompt input reaches the same executor path. |
| System tests should remain explicit about whether they run against the REST API directly or through `run21ctl`. | `REQ-EXEC-001`, `REQ-AUDIT-001` | Split API and CLI smoke checks into named tests that assert the same status and audit fields. |
