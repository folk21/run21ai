# ROADMAP

## Stage 1: Documentation and Contract Model

Status: done

Goals:

* Define the documentation profile.
* Define the abstract system core.
* Define dispatcher-driven tool execution terminology.
* Define dispatcher chain semantics.
* Define tool request/result contracts.
* Define audit and status models.
* Define requirement-to-test traceability.

Exit criteria:

* Requirements are mapped to tests.
* Core concepts are documented.
* Dispatcher chain behavior is documented.
* Tool execution terms are consistent.
* README explains the core execution model.

## Stage 2: Core Module

Status: in_progress

Goals:

* Create the framework-independent `core` module.
* Add core models and interfaces.
* Add dispatcher chain contracts.
* Add tool executor contracts.
* Add tool implementation contracts.
* Add registry contracts.
* Add unsupported default implementations.
* Add `ToolCalls` as the recommended high-level call API.
* Add contract tests.

Current scope:

* `ToolCalls.execute(...)` for structured calls.
* `ToolCalls.dispatch(...)` for raw dispatcher-chain calls.
* Pass-through dispatcher for direct structured execution.
* Ordered dispatcher chain.
* Tool registries and in-memory registry implementations.
* Default tool executor.
* Unsupported dispatcher/tool behavior.
* Audit metadata snapshots.
* Mock LLM dispatcher contract example.

Exit criteria:

* Core builds without runtime framework dependencies.
* Contract tests pass.
* Structured calls work through `ToolCalls.execute(...)`.
* Raw dispatcher-chain calls work through `ToolCalls.dispatch(...)`.
* Unsupported tools fail with structured errors.
* Tool results contain audit metadata.
* README examples match the recommended API.

## Stage 3: First Runtime Skeleton

Status: in_progress

Goals:

* Create a minimal runtime module outside `core`.
* Wire core services during application startup.
* Register an in-memory tool registry.
* Register a dispatcher chain profile.
* Register tool executors and implementations.
* Expose a minimal local execution path.

Current scope:

* `:runtime:local` module.
* `LocalRun21Runtime` facade over a wired `ToolExecutionService`.
* `LocalRun21RuntimeFactory.formulaOnly()` in-memory runtime profile.
* Code-based formula tool registration outside `core`.
* Structured local execution through `ToolCalls.execute(...)`.
* Contract-style local runtime test.
* `:runtime:rest-api` Ktor module.
* REST endpoints for health, readiness, tool discovery, request resolution, and execution.
* REST API tests for structured execution and current prompt-dispatch rejection behavior.

Exit criteria:

* A structured tool request can be executed through a runtime-wired dispatcher chain.
* A raw request can be dispatched through a runtime-wired dispatcher chain.
* Execution result contains status, output or error, and audit metadata.
* Runtime code depends on `core`; `core` does not depend on runtime code.
* REST endpoints expose runtime state and tool execution without changing core semantics.

## Stage 4: First Concrete Tool Module

Status: in_progress

Current scope:

* `:tools:formula` module.
* `formula.evaluate` tool descriptor and implementation.
* EvalEx-backed deterministic formula evaluation.
* Unit test for direct implementation execution.
* Integration-style tests through `ToolCalls.execute(...)`.

Goals:

* Add the first production tool implementation outside `core`.
* Start with a deterministic formula-oriented or BigDecimal-oriented tool.
* Support structured input payloads.
* Support deterministic output.
* Support version metadata.
* Add unit tests for the tool implementation.
* Add integration-style tests through `ToolCalls`.

Exit criteria:

* The concrete tool executes through the common pipeline.
* Tool-specific validation is covered by tests.
* Version and audit metadata are recorded in the result.
* No tool-specific execution path is introduced.

## Stage 5: Tool Registration Improvements

Status: planned

Goals:

* Define a simple runtime registration model.
* Keep code-based registration as the baseline.
* Prepare configuration-based registration for declarative tools.
* Keep complex and security-sensitive tools code-based.

Exit criteria:

* Runtime can register tools, executors, implementations, and dispatcher chains explicitly.
* Simple declarative tool registration is documented.
* Registration does not change core semantics.

## Stage 6: CLI Client

Status: planned

Goals:

* Add a Kotlin CLI client.
* Support health and tool execution commands.
* Support structured tool calls.
* Support raw dispatcher-chain calls.
* Display structured output, structured errors, and audit metadata.

Exit criteria:

* CLI can submit a structured tool request.
* CLI can submit prompt-like input through a dispatcher chain.
* CLI can display tool results and structured errors.

## Stage 7: LLM Dispatcher

Status: planned

Goals:

* Add a local LLM dispatcher implementation outside `core`.
* Support structured output validation.
* Support retry on invalid dispatcher output.
* Keep tool execution deterministic.
* Keep the LLM limited to request preparation.

Exit criteria:

* Unstructured input can be converted into a valid `ToolRequest`.
* Invalid LLM output is rejected before tool execution.
* LLM dispatcher does not perform tool work.
* Tool execution still uses `ToolExecutor` and `ToolImplementation`.

## Stage 8: Additional Tool Implementations

Status: planned

Goals:

* Add custom JVM tool implementations.
* Add spreadsheet-style tool implementation.
* Add HTTP/external service tool implementation.
* Add tool-specific tests.

Exit criteria:

* New tool implementations can be registered without changing core semantics.
* Each tool returns structured output or structured errors.
* Audit metadata remains consistent across tool families.
