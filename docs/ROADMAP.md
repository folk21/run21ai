# Roadmap

Status legend:

- `[x]` `DONE` — implemented foundation or completed work;
- `[/]` `IN_PROGRESS` — partially implemented or currently being hardened;
- `[ ]` `TODO` — planned work that has not been completed.

Completed release history belongs in [CHANGELOG.md](CHANGELOG.md). This file keeps a compact foundation summary plus current and planned work.

## Completed Foundations

- [x] Define the framework-independent `:core` execution model.
- [x] Route structured and raw input through the same `DispatcherChain` boundary.
- [x] Provide canonical `ToolRequest`, `ToolResult`, `ToolError`, and audit models.
- [x] Provide `ToolCalls.execute(...)` and `ToolCalls.dispatch(...)` as the recommended high-level APIs.
- [x] Provide ordered dispatching, pass-through structured dispatching, registries, default execution, and unsupported defaults.
- [x] Preserve caller actor, source, and correlation provenance across dispatcher enrichment and acceptance.
- [x] Add deterministic core contract tests without runtime framework dependencies.
- [x] Add `:runtime:local` and the Ktor-based `:runtime:rest-api` runtime.
- [x] Expose health, readiness, tool discovery, request resolution, execution, OpenAPI, and interactive API documentation endpoints.
- [x] Add the deterministic EvalEx-backed `formula.evaluate` tool outside `:core`.
- [x] Add the provider-neutral `:dispatchers:llm` module and the Ollama adapter in `:dispatchers:llm-ollama`.
- [x] Keep LLM behavior limited to preparing or rejecting a `ToolRequest`; registered tools still perform all real work.
- [x] Add the independent Rust `run21ctl` client and pytest-based system-test foundation.
- [x] Consolidate project documentation into owning documents with machine-readable contracts kept in runtime resources.

## Execution Hardening

- [x] Normalize dispatcher, dispatcher-chain, executor, and implementation failures into structured results with audit metadata.
- [x] Add stable dispatcher, tool-execution, and timeout error codes; propagate cancellation without converting it into a result.
- [x] Use one injected clock for audit start and completion timestamps across default runtime wiring.
- [/] Improve core and formula validation diagnostics while preserving stable public error contracts.
- [/] Define which `CapabilityStatus` values are informational and which block execution.
- [/] Decide whether `ContractSchema` is descriptive metadata or an enforced runtime contract; prefer validation before execution.
- [ ] Validate that tool inputs and outputs are JSON-compatible before they reach transport serialization.
- [ ] Make readiness validate the complete registration graph rather than descriptor count only.
- [ ] Reject unknown HTTP enum values instead of silently substituting defaults.
- [/] Make default implementation selection explicit and detect duplicate registrations during runtime assembly.
- [x] Replace blocking Ollama transport calls with asynchronous execution.
- [x] Redact sensitive prompt and model-response data from dispatcher debug logging.
- [/] Activate deterministic raw-dispatch system coverage and remove obsolete `xfail` expectations.
- [ ] Add one reproducible repository-level validation command and run it in continuous integration.

## Generic and Safe LLM Dispatch

- [/] Strengthen structured-output validation beyond the current formula-specific Ollama response checks.
- [ ] Build the model-visible tool catalog from registered descriptors instead of hardcoded formula constants.
- [ ] Apply caller-specific tool allowlists before sending a catalog to a model.
- [ ] Keep implementation selection under runtime policy by default rather than model control.
- [ ] Validate accepted model output against the selected tool contract.
- [ ] Verify exact user-provided business values, not only parameter names.
- [ ] Add bounded repair retries for invalid or ambiguous structured output.
- [ ] Record provider, model, prompt version, attempt count, and latency in dispatcher trace metadata.
- [ ] Add optional Ollama integration tests without adding network dependencies to deterministic core tests.

## Registration and Tool Growth

- [/] Consolidate runtime registration around one explicit composition model and remove duplicated formula wiring.
- [ ] Add a second tool family that creates different runtime pressure, such as an allowlisted HTTP adapter or deterministic document transformation.
- [ ] Add tool-specific validation, timeout behavior, negative-path tests, REST coverage, and CLI smoke coverage for the second tool.
- [ ] Keep complex and security-sensitive tools code-registered.
- [ ] Evaluate declarative registration only after multiple low-risk tools share a proven configuration contract.

## Clients and System Validation

- [/] Harden `run21ctl` error handling and add friendlier output modes only where raw JSON is insufficient.
- [/] Add CLI contract tests for invalid arguments and structured backend errors.
- [/] Keep CLI commands, REST mappings, OpenAPI, and usage examples synchronized.
- [/] Run prompt-resolution system tests as normal tests when a deterministic raw-dispatch profile is available.
- [ ] Decide which Rust and pytest checks are required in continuous integration.

## Operational Platform

- [ ] Add persistent execution and audit storage after the in-memory contracts stabilize.
- [ ] Add authentication, authorization, and per-actor tool policy.
- [ ] Add metrics, tracing, rate limits, execution budgets, and operational diagnostics.
- [ ] Add explicit deployment configuration profiles.

## Bounded Orchestration

- [ ] Evaluate workflow or graph execution only after several independent tools are stable.
- [ ] Require orchestration to call the existing execution service with ordinary `ToolRequest` values.
- [ ] Keep autonomous agent loops, conversational memory, unrestricted recursive tool calls, and model-driven execution outside `:core`.
