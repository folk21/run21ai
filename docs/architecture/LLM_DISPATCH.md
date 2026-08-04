# LLM Dispatch

An LLM is an optional dispatcher implementation. It prepares a candidate `ToolRequest`; it never performs the requested tool work.

## Boundary

An LLM dispatcher may:

- analyze raw input;
- identify an intended tool;
- extract and normalize arguments;
- add request or trace metadata;
- reject ambiguous, incomplete, invalid, or unsupported input.

An LLM dispatcher must not:

- calculate the final business result;
- call a `ToolImplementation`;
- bypass `ToolExecutor` or audit metadata;
- perform external operations unless the operation is implemented as a registered tool;
- invent missing business values.

## Modules

| Module | Responsibility |
|---|---|
| `:dispatchers:llm` | Provider-neutral prompt/response contracts, mapping to `ToolRequest`, and `LlmDispatcher` |
| `:dispatchers:llm-ollama` | Ollama configuration, Java HTTP transport, prompt resources, JSON schema, response parsing, and formula-specific normalization |
| `:runtime:rest-api` | Chooses whether the formula-only or Ollama dispatcher profile is active |

Provider-specific code must not leak into `:core`.

## Current Flow

```text
RawDispatchInput
  -> LlmDispatcher
  -> LlmDispatchClient
  -> Ollama prompt and structured-output schema
  -> response normalization
  -> LlmDispatchAccepted or LlmDispatchRejected
  -> ToolRequest validation
  -> DispatchAccepted or DispatchRejected
```

After `DispatchAccepted`, execution continues through the ordinary execution contracts described in [Execution Contracts](EXECUTION.md).

## Current Ollama Profile

The current Ollama adapter is intentionally narrow:

- it supports `formula.evaluate` only;
- prompt resources contain the allowed tool and implementation identifiers;
- the model must return JSON matching the adapter schema;
- output is normalized before it reaches the provider-neutral parser;
- missing variables, unsupported tools, missing expressions, ambiguous requests, and invalid model output are rejected.

Runtime configuration is documented in [Installation](../INSTALLATION.md).

## Structured Output Ownership

The machine-readable schema is owned by:

```text
dispatchers/llm-ollama/src/main/resources/ollama/formula-dispatch/output-schema.json
```

The prompt templates are owned by the same resource directory. Do not copy their fields into a second JSON schema under `docs/`. Tests for prompt construction, parsing, and normalization must change with these resources.

## Validation Layers

Model output is untrusted and passes through several checks:

1. Ollama is asked for schema-constrained JSON.
2. The adapter parses the response as JSON.
3. Formula-specific normalization accepts only known decisions and rejection reasons.
4. Required expression and variable names are checked.
5. The provider-neutral response is mapped to a `ToolRequest`.
6. Core request validation runs before the dispatcher accepts the request.
7. Tool-specific validation runs again before implementation execution.

The current validator compares required and returned variable names with names found in the prompt. It does not yet independently prove that every returned value exactly matches the user's value. Exact value-integrity validation is a roadmap requirement.

## Failure Behavior

- An explicit model rejection becomes `DispatchRejected`.
- Invalid structured output becomes `INVALID_TOOL_REQUEST` or a normalized model rejection.
- Provider or transport failures propagate to the dispatcher chain and are normalized as `DISPATCHER_FAILED`.
- Provider or transport timeouts are normalized as `TIMEOUT`.
- Caller cancellation propagates without being converted into a dispatch rejection.
- Public errors do not expose raw provider exception classes or messages.
- No tool implementation is invoked after dispatch rejection.

## Safety and Operational Rules

- Use temperature `0.0` for deterministic request preparation unless a validated use case requires otherwise.
- Keep retry count bounded when retries are introduced.
- Apply runtime allowlists before exposing tools to a model.
- Keep implementation selection under runtime policy by default.
- Do not log raw prompts or model responses containing sensitive data.
- Record model and prompt metadata in trace snapshots when the adapter supports it.
- Keep external-model integration tests optional; deterministic tests use fake clients and fixed responses.

## Direction

The next design step is a registry-driven, caller-specific tool catalog with schema validation and runtime policy. Run21AI should remain a controlled execution substrate rather than becoming an autonomous agent loop or unrestricted planner.
