# ROADMAP

## Table of Contents

* [Stage 1: Documentation and Contract Model - done](#stage-1-documentation-and-contract-model)
* [Stage 2: Core Module - in_progress](#stage-2-core-module)
* [Stage 3: First Runtime Skeleton - in_progress](#stage-3-first-runtime-skeleton)
* [Stage 4: First Concrete Tool Module - in_progress](#stage-4-first-concrete-tool-module)
* [Stage 5: Tool Registration Improvements - planned](#stage-5-tool-registration-improvements)
* [Stage 6: CLI Client - in_progress](#stage-6-cli-client)
* [Stage 7: LLM Dispatcher - in_progress](#stage-7-llm-dispatcher)
* [Stage 8: Additional Tool Implementations - planned](#stage-8-additional-tool-implementations)
* [Stage 9: System Tests - in_progress](#stage-9-system-tests)

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

Remaining work:

* No planned feature work.
* Keep documentation aligned when public terminology, runtime behavior, or API contracts change.

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

Remaining work:

* Improve error diagnostics and exception normalization in the execution path.
* Add more focused unit and contract tests for validation, registries, status behavior, and audit metadata.
* Clarify which capability statuses are informational and which should block execution.
* Keep core APIs stable while runtime, REST, CLI, and tool modules put pressure on the model.

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

Remaining work:

* Add a raw/prompt dispatcher profile so raw requests can resolve into ToolRequest.
* Improve runtime composition so additional tools can be registered without duplicating wiring code.
* Decide how runtime profiles should be selected for local, REST, CLI, tests, and future deployment.
* Keep OpenAPI, REST tests, and README examples synchronized with implemented endpoints.

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

Remaining work:

* Broaden formula validation and diagnostics for invalid expressions, invalid variables, and engine failures.
* Add more edge-case tests for numeric behavior, rounding, unsupported result types, and malformed payloads.
* Decide the public contract for formula-specific error details.
* Keep the formula module free of runtime, REST, CLI, and dispatcher concerns.

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

Remaining work:

* Define a reusable registration model for multiple tools and implementations.
* Keep code-based registration as the safe baseline.
* Explore simple declarative registration only for low-risk tool metadata.
* Document which tool types must remain code-registered for security or operational reasons.

## Stage 6: CLI Client

Status: in_progress

Goals:

* Add a CLI client.
* Support health and tool execution commands.
* Support structured tool calls.
* Support raw dispatcher-chain calls.
* Display structured output, structured errors, and audit metadata.

Current scope:

* `clients/run21ctl` Rust CLI client.
* Health, readiness, tool listing, tool descriptor lookup, execution, and request resolution commands.
* Cargo-based CLI contract tests outside the Gradle build.
* REST API backend URL configuration through `--base-url` or `RUN21_BASE_URL`.

Exit criteria:

* CLI can submit a structured tool request.
* CLI can submit prompt-like input through a dispatcher chain.
* CLI can display tool results and structured errors.

Remaining work:

* Verify and harden the Rust CLI once Cargo is available in the local environment.
* Add friendlier CLI output modes if raw JSON becomes hard to scan.
* Add more CLI contract tests for error responses and invalid command arguments.
* Keep CLI commands aligned with REST API and OpenAPI changes.

## Stage 7: LLM Dispatcher

Status: in_progress

Goals:

* Add a local LLM dispatcher implementation outside `core`.
* Support structured output validation.
* Support retry on invalid dispatcher output.
* Keep tool execution deterministic.
* Keep the LLM limited to request preparation.

Current scope:

* `:dispatchers:llm` module.
* `:dispatchers:llm-ollama` module.
* Provider-neutral `LlmDispatchClient` abstraction.
* `LlmDispatcher` implementation of the core `Dispatcher` contract.
* Deterministic fake-client tests without a real LLM, network access, or credentials.
* Structured accepted/rejected LLM-style responses mapped into core dispatcher outcomes.
* Ollama adapter configured by environment variables.
* Optional REST runtime profile enabled with `RUN21_REST_PROFILE=ollama`.

Exit criteria:

* Unstructured input can be converted into a valid `ToolRequest`.
* Invalid LLM output is rejected before tool execution.
* LLM dispatcher does not perform tool work.
* Tool execution still uses `ToolExecutor` and `ToolImplementation`.

Remaining work:

* Add stricter structured-output schema validation for provider responses.
* Add retry behavior for invalid or ambiguous model output.
* Add optional integration/system tests that run only when a real Ollama server is available.
* Generalize prompt construction beyond the current formula-oriented tool description.
* Keep LLM dispatch outside `core` and outside tool implementation work.

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

Remaining work:

* Choose the next tool family based on the strongest runtime pressure.
* Add one JVM/custom tool and one adapter-style tool before broadening abstractions.
* Add tool-specific validation, execution tests, and REST/CLI smoke coverage.
* Confirm that new tools do not require core semantic changes.

## Stage 9: System Tests

Status: in_progress

Current scope:

* `tests/system` pytest module.
* HTTP API system tests for health, readiness, tool discovery, formula execution, and structured errors.
* Rust CLI smoke tests through `run21ctl`.
* Optional backend startup through `RUN21_START_SERVER=true`.
* Prompt-resolution future-contract tests marked as expected failures until a raw dispatcher is registered.

Exit criteria:

* System tests can run against a locally started REST backend.
* CLI smoke tests can run against a built `run21ctl` binary.
* Prompt-resolution tests become active when a raw/prompt dispatcher is implemented.

Remaining work:

* Run the full system suite in an environment with pytest, Cargo, and the REST backend available.
* Convert prompt-resolution `xfail` tests into normal tests after a raw dispatcher is registered.
* Add negative-path system tests for unknown tools, invalid payloads, and CLI error handling.
* Decide whether system tests should become part of CI or remain an explicit local verification suite.
