# Roadmap

Run21AI is moving from a working vertical slice toward a reliable, policy-controlled tool execution engine. This file contains future work only; completed changes belong in [Changelog](CHANGELOG.md).

## Now — Execution Hardening

Goal: make every execution path controlled, reproducible, and operationally safe.

- Normalize dispatcher and tool implementation exceptions into stable `ToolResult` errors.
- Define execution error codes for internal failures, timeouts, and cancellation.
- Make readiness validate the complete registration graph rather than descriptor count only.
- Reject unknown HTTP enum values instead of silently substituting defaults.
- Make default implementation selection explicit and detect duplicate registrations at startup.
- Clarify whether `ContractSchema` is descriptive metadata or an enforced runtime contract; prefer enforcement before execution.
- Validate that tool inputs and outputs are JSON-compatible at transport boundaries.
- Replace blocking Ollama transport calls with asynchronous execution.
- Redact or disable full prompt and model-response logging by default.
- Activate deterministic raw-dispatch system coverage and remove obsolete `xfail` expectations.

Exit criteria:

- expected failures return structured results with audit metadata;
- runtime composition fails fast when registrations are inconsistent;
- the full Gradle suite and client/system checks are reproducible from a clean checkout.

## Next — Generic and Safe LLM Dispatch

Goal: support more than one tool without making the LLM an executor or policy authority.

- Build the LLM tool catalog from registered descriptors instead of hardcoded formula constants.
- Apply caller-specific tool allowlists before prompting a model.
- Keep implementation selection under runtime policy by default.
- Validate model output against the selected tool contract.
- Verify exact user-provided values, not only parameter names.
- Add bounded repair retries for invalid structured output.
- Record provider, model, prompt version, attempt count, and latency in dispatcher trace metadata.
- Add optional integration tests for an available Ollama server without making them part of deterministic core tests.

Exit criteria:

- a model cannot select an unavailable or unauthorized tool;
- a model cannot invent missing business values;
- accepted model output uses the same `ToolRequest`, executor, result, and audit contracts as structured execution.

## Next — Registration and Second Tool

Goal: prove the architecture with another tool family before generalizing it further.

- Consolidate runtime registration around one explicit composition model.
- Add one tool that creates different runtime pressure, such as an allowlisted HTTP adapter or deterministic document transformation.
- Add tool-specific validation, timeout behavior, negative-path tests, REST coverage, and CLI smoke coverage.
- Keep declarative registration limited to low-risk metadata; keep sensitive behavior code-registered.

Exit criteria:

- a second tool is added without changing core execution semantics;
- runtime composition does not duplicate descriptor, executor, implementation, or validator wiring.

## Later — Operational Platform

- Persistent execution and audit storage.
- Authentication, authorization, and per-actor tool policy.
- Metrics, tracing, rate limits, and execution budgets.
- Configuration profiles for deployment environments.
- Additional client output modes and operational diagnostics.

## Later — Bounded Orchestration

Consider workflow or graph execution only after several independent tools are stable. Orchestration must call the existing execution service with ordinary `ToolRequest` values and must not introduce a second tool execution path.

Out of scope for the core:

- autonomous agent loops;
- conversational memory;
- unrestricted recursive tool calls;
- model-driven execution that bypasses runtime policy.
