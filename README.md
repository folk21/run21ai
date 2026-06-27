# Run21AI

Run21AI is a lightweight dispatcher-driven tool execution engine.

The system is based on one execution model:

```text
Input
  -> DispatcherChain
  -> ToolRequest
  -> ToolExecutor
  -> ToolImplementation
  -> ToolResult
```

A dispatcher chain is always part of execution. For direct structured calls, the chain may contain only a pass-through dispatcher. For unstructured input, the chain may include rule-based, LLM-based, validation, enrichment, or custom dispatchers.

Tools perform real work. Dispatchers analyze, normalize, enrich, validate, or route input into a structured `ToolRequest`.

Run21AI is domain-neutral. It can execute formula-oriented tools, document tools, lookup tools, workflow tools, spreadsheet-style tools, external service tools, and custom tools.

## Project Navigation

- [Roadmap](ROADMAP.md)
- [Documentation Index](docs/INDEX.md)
- [Requirements](docs/01-requirements/README.md)
- [Core Documentation](docs/02-core/README.md)
- [Architecture](docs/03-architecture/README.md)
- [API Documentation](docs/05-api/README.md)
- [Tool Documentation](docs/06-tools/README.md)
- [Testing Documentation](docs/08-testing/README.md)

## Table of Contents

- [Project Navigation](#project-navigation)
- [Core Idea](#core-idea)
- [Execution Modes](#execution-modes)
- [Runtime Modules](#runtime-modules)
- [Dispatcher Modules](#dispatcher-modules)
- [Manual REST API Testing](#manual-rest-api-testing)
- [Rust CLI Client](#rust-cli-client)
- [System Tests](#system-tests)
- [Core Package Overview](#core-package-overview)
- [Example: Direct Tool Execution With BigDecimal Rounding](#example-direct-tool-execution-with-bigdecimal-rounding)
- [Example: Mock LLM Dispatcher Calling the Same Tool](#example-mock-llm-dispatcher-calling-the-same-tool)
- [Dispatcher Chain Example](#dispatcher-chain-example)
- [Formula Tool Module](#formula-tool-module)
- [Build](#build)

## Core Idea

Run21AI separates request preparation from tool execution. The core module is the abstract execution kernel that defines how callers, dispatchers, executors, and implementations interact.

The central runtime path is:

```text
External caller
  -> DispatchInput
  -> ToolExecutionService
  -> DispatcherChain
  -> ToolRequest
  -> ToolExecutor
  -> ToolImplementation
  -> ToolExecutionOutput
  -> ToolResult
```

### 1. Wiring and bootstrap

Before tools can be called, a runtime module wires the execution environment. Wiring is the process of registering available tools, their implementations, executors, registries, dispatcher chains, and the final execution service.

Typical wiring creates these components:

```text
ToolDescriptor
  describes what tool exists

ToolImplementation
  performs the actual deterministic work

ToolImplementationRegistry
  knows available implementations

ToolExecutor
  validates a request and delegates to an implementation

ToolExecutorRegistry
  knows available executors

ToolRegistry
  knows available tools

DispatcherChain
  converts input into a ToolRequest

ToolExecutionService
  acts as the main entry point
```

After wiring, external layers such as CLI, HTTP API, tests, workers, or LLM integrations call a ready `ToolExecutionService`.

### 2. Tool registration

A tool is described by a `ToolDescriptor`. The descriptor contains metadata such as code, name, description, input schema, output schema, and status.

A concrete implementation is represented by a `ToolImplementation`. For example, a rounding tool can be implemented by a deterministic JVM class that receives numeric values and returns rounded values.

The descriptor and implementation are stored in registries:

```text
ToolRegistry
  stores ToolDescriptor objects

ToolImplementationRegistry
  stores ToolImplementation objects

ToolExecutorRegistry
  stores ToolExecutor objects
```

The core module provides in-memory registries for local wiring and tests. Runtime modules can replace them with framework-managed or persistent registries.

### 3. ToolExecutor and ToolImplementation

`ToolExecutor` and `ToolImplementation` have different responsibilities.

`ToolExecutor` owns the standard execution protocol:

- checks that the request targets the expected tool;
- validates the request;
- resolves the requested implementation;
- handles missing implementations;
- invokes the implementation;
- wraps the output into a `ToolResult`;
- adds audit metadata.

`ToolImplementation` owns the actual deterministic work of a specific tool.

For example:

```text
DefaultToolExecutor
  -> BigDecimalRoundingToolImplementation
```

This keeps common execution behavior centralized while allowing each tool implementation to remain focused on business logic.

### 4. DispatcherChain

`DispatcherChain` converts external input into a normalized `ToolRequest`.

The system supports two main input shapes:

```text
Structured input
  the caller already knows which tool to call

Raw input
  the caller provides prompt-like or partially structured input
```

For structured calls, the default chain can use `PassThroughDispatcher`. It accepts an existing `ToolRequest` and forwards it unchanged.

For raw calls, the chain can include rule-based, custom, validation, enrichment, or LLM-style dispatchers. These dispatchers analyze the input and produce the same `ToolRequest` model used by structured calls.

### 5. Runtime execution: structured tool call

When a caller uses `ToolCalls.execute(...)`, the builder creates a `ToolRequest`, wraps it into `StructuredDispatchInput`, and sends it to `ToolExecutionService`.

The runtime path is:

```text
ToolCalls.execute
  -> StructuredDispatchInput
  -> PassThroughDispatcher
  -> ToolRequest
  -> DefaultToolExecutionService
  -> DefaultToolExecutor
  -> ToolImplementation
  -> ToolResult
```

The pass-through dispatcher does not modify the request. It only makes structured calls use the same dispatcher-chain boundary as every other execution mode.

### 6. Runtime execution: raw or LLM-style call

When a caller uses `ToolCalls.dispatch(...)`, the builder creates `RawDispatchInput` instead of a ready `ToolRequest`.

The runtime path is:

```text
ToolCalls.dispatch
  -> RawDispatchInput
  -> Rule-based / custom / LLM dispatcher
  -> ToolRequest
  -> DefaultToolExecutionService
  -> DefaultToolExecutor
  -> ToolImplementation
  -> ToolResult
```

The dispatcher prepares the `ToolRequest`. It does not execute the tool. After the request is produced, the execution path is the same as in structured execution.

This means structured calls and LLM-dispatched calls share the same request model, executor contract, implementation contract, result model, error model, and audit model.

## Execution Modes

Run21AI has one execution pipeline and two primary ways to enter it.

### Structured execution

Structured execution is used when the caller already knows the target tool, optional implementation, and payload.

```text
ToolCalls.execute
  -> StructuredDispatchInput
  -> PassThroughDispatcher
  -> ToolRequest
  -> ToolExecutor
  -> ToolImplementation
  -> ToolResult
```

This mode is useful for CLI commands, API endpoints, tests, workers, and other callers that can submit a complete request directly.

### Dispatcher-based execution

Dispatcher-based execution is used when the caller submits raw or partially structured input.

```text
ToolCalls.dispatch
  -> RawDispatchInput
  -> DispatcherChain
  -> ToolRequest
  -> ToolExecutor
  -> ToolImplementation
  -> ToolResult
```

This mode is useful for rule-based routing, custom request enrichment, validation chains, and LLM-style prompt analysis.

## Runtime Modules

Runtime modules assemble core contracts and concrete tools into ready execution services.

### `:runtime:local`

The local runtime module is the first composition module. It depends on `:core` and concrete tool modules, wires in-memory registries, registers tool executors and implementations, selects a dispatcher chain profile, and exposes a ready `ToolExecutionService`.

The module currently provides `LocalRun21RuntimeFactory.formulaOnly()`, which registers the formula evaluation tool with the pass-through dispatcher chain for structured calls.

```kotlin
val runtime = LocalRun21RuntimeFactory.formulaOnly()

val result = runtime.execute {
    tool(FormulaTool.TOOL_CODE)
    implementation(FormulaTool.IMPLEMENTATION_CODE)
    actor("user-1", ActorType.HUMAN, "User")
    source(ExecutionSourceType.TEST, "local-runtime-example")
    input(FormulaTool.Input.EXPRESSION, "a + b")
    input(
        FormulaTool.Input.VARIABLES,
        mapOf(
            "a" to BigDecimal("10"),
            "b" to BigDecimal("11"),
        ),
    )
    correlationId("local-runtime-example")
}
```

`ToolCalls` is used at the runtime boundary through `LocalRun21Runtime.execute(...)` and `LocalRun21Runtime.dispatch(...)`. Runtime construction itself still uses explicit registries, executors, implementations, and dispatcher chains.

### `:runtime:rest-api`

The REST API runtime exposes the local runtime through Ktor. It is a runtime module, so Ktor dependencies, HTTP DTOs, JSON mapping, and endpoint routing stay outside `core`.

Run the server locally from the project root:

```bash
./gradlew :runtime:rest-api:run
```

The server listens on `0.0.0.0:8080` by default. Override it with `RUN21_REST_HOST` and `RUN21_REST_PORT`.

Available endpoints:

```text
GET  /health
GET  /ready
GET  /docs
GET  /openapi.yaml
GET  /v1/tools
GET  /v1/tools/{code}
POST /v1/tool-requests/resolve
POST /v1/executions
```

Open the interactive API documentation in a browser:

```text
http://127.0.0.1:8080/docs
```

The raw OpenAPI contract is available at:

```text
http://127.0.0.1:8080/openapi.yaml
```

`POST /v1/executions` accepts a structured or prompt-like request. Structured requests execute through the same dispatcher-chain path as `ToolCalls.execute(...)`.

```json
{
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
    "id": "user-1",
    "type": "human",
    "displayName": "User"
  },
  "source": {
    "type": "api",
    "name": "rest-api-example"
  },
  "correlation": {
    "correlationId": "rest-api-example"
  }
}
```

`POST /v1/tool-requests/resolve` runs only the dispatcher chain and returns a prepared `ToolRequest` or a structured dispatch error. With the current formula-only runtime profile, prompt-like requests are rejected because no raw-input dispatcher is registered yet.

## Dispatcher Modules

### `:dispatchers:llm`

The LLM dispatcher module provides a provider-neutral dispatcher for prompt-like input. It depends
on `:core` and implements the core `Dispatcher` contract. It does not include provider-specific
HTTP code.

The current implementation uses an injected `LlmDispatchClient` interface:

```text
RawDispatchInput
  -> LlmDispatcher
  -> LlmDispatchClient
  -> LlmDispatchResponse
  -> ToolRequest / DispatchRejected
```

Tests use deterministic fake clients, so this module does not require network access, API keys, or
model availability. Provider adapters can be added without changing `core`.

### `:dispatchers:llm-ollama`

The Ollama adapter module connects the provider-neutral LLM dispatcher to an Ollama-compatible
`/api/chat` endpoint. It uses Java HttpClient, Jackson, StringTemplate-rendered prompt resources,
and Ollama structured output, and it remains outside `core`.

The adapter asks the model to return dispatcher JSON only, then validates and normalizes the model
response before it reaches the provider-neutral dispatcher mapper. The model may select a target
tool, prepare input payload, enrich metadata, or reject ambiguous input. It must not execute tools
or calculate final results.

Configuration is environment-driven:

```text
RUN21_LLM_BASE_URL=http://localhost:11434
RUN21_LLM_MODEL=<installed-ollama-model>
RUN21_LLM_TIMEOUT_MS=30000
RUN21_LLM_TEMPERATURE=0.0
```

`RUN21_LLM_MODEL` is required. Run21AI does not choose a default model because model availability
depends on the local or remote Ollama installation.

Unit tests for this module use a fake transport and do not require Ollama to be running.

## Manual REST API Testing

Run the commands in this section from the project root unless noted.

Start the backend:

```bash
./gradlew :runtime:rest-api:run
```

By default, the Ktor server listens on:

```text
http://127.0.0.1:8080
```

Use another port when needed:

```bash
RUN21_REST_PORT=9090 ./gradlew :runtime:rest-api:run
```

The default REST runtime profile is formula-only. To enable prompt resolution through Ollama, start
Ollama in one terminal and run the backend with the Ollama profile in another terminal:

```bash
ollama run <installed-ollama-model>
```

```bash
RUN21_REST_PROFILE=ollama \
RUN21_LLM_BASE_URL=http://localhost:11434 \
RUN21_LLM_MODEL=<installed-ollama-model> \
./gradlew :runtime:rest-api:run
```

Then resolve prompt-like input through the dispatcher chain:

```bash
curl -X POST http://127.0.0.1:8080/v1/tool-requests/resolve \
  -H 'Content-Type: application/json' \
  -d '{
    "type": "prompt",
    "prompt": "Prepare a formula request for a + b where a is 10 and b is 11.",
    "actor": {
      "id": "manual-user",
      "type": "human"
    },
    "source": {
      "type": "api",
      "name": "curl"
    },
    "correlation": {
      "correlationId": "ollama-prompt-resolve-test"
    }
  }'
```

Basic checks:

```bash
curl http://127.0.0.1:8080/health
curl http://127.0.0.1:8080/ready
curl http://127.0.0.1:8080/openapi.yaml
curl http://127.0.0.1:8080/v1/tools
curl http://127.0.0.1:8080/v1/tools/formula.evaluate
```

Execute the formula tool:

```bash
curl -X POST http://127.0.0.1:8080/v1/executions \
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
      "id": "manual-user",
      "type": "human",
      "displayName": "Manual User"
    },
    "source": {
      "type": "api",
      "name": "curl"
    },
    "correlation": {
      "correlationId": "manual-curl-test"
    }
  }'
```

Resolve the same request without executing the tool:

```bash
curl -X POST http://127.0.0.1:8080/v1/tool-requests/resolve \
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
      "id": "manual-user",
      "type": "human"
    },
    "source": {
      "type": "api",
      "name": "curl"
    }
  }'
```

## Rust CLI Client

`clients/run21ctl` is an independent Rust CLI client for the Run21AI REST API. It is not included in
the Gradle build.

Run the Cargo examples in this section from the project root so the `clients/run21ctl/Cargo.toml`
manifest path resolves correctly.

Cargo is installed with the Rust toolchain. On macOS or Linux, install it with `rustup`:

```bash
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
```

Then open a new shell or reload your shell profile and verify:

```bash
cargo --version
```

Run its tests with Cargo:

```bash
cargo test --manifest-path clients/run21ctl/Cargo.toml
```

Call the backend health endpoint:

```bash
cargo run --manifest-path clients/run21ctl/Cargo.toml -- health
```

The client defaults to `http://127.0.0.1:8080`. Override the backend URL with `--base-url` or
`RUN21_BASE_URL`.

Useful commands:

```bash
cargo run --manifest-path clients/run21ctl/Cargo.toml -- ready
cargo run --manifest-path clients/run21ctl/Cargo.toml -- tools
cargo run --manifest-path clients/run21ctl/Cargo.toml -- tool formula.evaluate
```

Execute the formula tool through the REST API:

```bash
cargo run --manifest-path clients/run21ctl/Cargo.toml -- execute \
  --tool-code formula.evaluate \
  --implementation-code formula-evalex-v1 \
  --input-json '{"expression":"a + b","variables":{"a":10,"b":11}}' \
  --actor-id manual-user \
  --correlation-id run21ctl-manual-test
```

See [clients/run21ctl/README.md](clients/run21ctl/README.md) for the full CLI command list.

## System Tests

`tests/system` contains pytest-based external contract tests for the REST API and Rust CLI.
They verify the runtime from outside the JVM process, so they are closer to black-box system tests
than unit or integration tests.

The tests exercise the deployed path from outside the process:

```text
pytest
  -> HTTP API / Rust CLI
  -> runtime:rest-api
  -> DispatcherChain
  -> ToolExecutor
  -> ToolImplementation
  -> ToolResult
```

Create a Python virtual environment and install the test dependencies:

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -e tests/system
```

Run the pytest examples in this section from the project root after activating the virtual
environment. The pytest configuration is in `tests/system/pytest.ini`, so the commands pass it
explicitly with `-c`.

There are two supported ways to run the tests.

Run against a backend that you started yourself:

```bash
./gradlew :runtime:rest-api:run
```

```bash
python -m pytest -c tests/system/pytest.ini tests/system/tests
```

In this mode pytest does not start the backend. It waits for `RUN21_BASE_URL`, which defaults to
`http://localhost:8080`.

Let pytest start and stop the backend for the test session:

```bash
RUN21_START_SERVER=true \
python -m pytest -c tests/system/pytest.ini tests/system/tests
```

Use `RUN21_BASE_URL` for the test client and `RUN21_REST_PORT` for the backend when the backend
listens on a custom port:

```bash
RUN21_BASE_URL=http://localhost:9090 \
RUN21_START_SERVER=true \
RUN21_SERVER_CMD='./gradlew :runtime:rest-api:run' \
RUN21_REST_PORT=9090 \
python -m pytest -c tests/system/pytest.ini tests/system/tests
```

Useful configuration:

```text
RUN21_BASE_URL=http://localhost:8080
RUN21_CLI_BIN=./clients/run21ctl/target/debug/run21ctl
RUN21_START_SERVER=false
RUN21_SERVER_CMD=./gradlew :runtime:rest-api:run
```

The current suite checks health and readiness endpoints, tool listing and descriptor schemas,
successful structured formula execution, structured errors for invalid formula input, CLI smoke
coverage when `run21ctl` is built, and prompt-resolution contracts that are marked `xfail` until a
raw/prompt dispatcher is enabled in the REST runtime profile.

See [tests/system/README.md](tests/system/README.md) for the full workflow, configuration details,
and test-case matrix.

## Core Package Overview

### `ai.run21.core`

Contains shared defaults for assembling a minimal core pipeline. The main helper is `CoreDefaults.structuredDirectDispatcherChain()`, which creates a dispatcher chain with only the pass-through dispatcher.

### `ai.run21.core.model`

Contains the canonical data model shared by all core packages: identifiers, actors, execution sources, dispatcher inputs and outcomes, tool descriptors, implementation descriptors, tool requests, tool results, errors, audit metadata, schemas, and capability statuses.

Key models:

- `DispatchInput` — external input accepted by the dispatcher chain.
- `StructuredDispatchInput` — input that already contains a complete `ToolRequest`.
- `RawDispatchInput` — unstructured input intended for a custom, rule-based, or LLM dispatcher.
- `ToolRequest` — the normalized request that identifies the target tool, optional implementation, payload, actor, source, trace, and metadata.
- `ToolResult` — the canonical execution result with status, output, error, and audit metadata.

### `ai.run21.core.dispatch`

Defines the dispatcher layer. A dispatcher receives `DispatchInput` and either accepts it by producing a `ToolRequest`, enriches the input, rejects it, or passes it to the next dispatcher.

The default direct execution path uses `PassThroughDispatcher`, which accepts `StructuredDispatchInput` without modifying the embedded request.

### `ai.run21.core.execution`

Defines the execution layer. `ToolExecutionService` is the public entry point of the core module. `DefaultToolExecutionService` coordinates dispatcher-chain resolution, tool lookup, executor lookup, unsupported defaults, and final result creation.

`ToolExecutor` validates and routes a request for one tool. `ToolImplementation` is the extension point where concrete runtime modules perform real work.

`ToolCalls`, `ToolCallBuilder`, and `RawToolCallBuilder` provide compact call APIs over the same canonical execution model. `ToolCalls.execute(...)` builds a structured `ToolRequest` and wraps it as `StructuredDispatchInput`; `ToolCalls.dispatch(...)` builds raw dispatcher input for rule-based, custom, or LLM-style dispatchers.

### `ai.run21.core.registry`

Contains registry contracts and in-memory implementations for tool descriptors, tool executors, and tool implementations. These registries are useful for tests, local wiring, and early runtime modules. They can later be replaced by persistent or framework-managed registries.

### `ai.run21.core` test package

Contains contract tests for the core execution model. The tests use deterministic test-only tool implementations and low-level coroutine execution to keep the core module lightweight.

## Example: Direct Tool Execution With BigDecimal Rounding

This example defines a deterministic tool that rounds four accounting-like `BigDecimal` values to a requested scale. The call uses `ToolCalls.execute(...)`, so the call site stays compact while the request still flows through `PassThroughDispatcher`, `DefaultToolExecutionService`, `DefaultToolExecutor`, and the concrete implementation.

### Tool implementation

```kotlin
import ai.run21.core.model.CapabilityStatus
import ai.run21.core.model.ImplementationCode
import ai.run21.core.model.ToolCode
import ai.run21.core.model.ToolExecutionOutput
import ai.run21.core.model.ToolImplementationDescriptor
import ai.run21.core.model.ToolImplementationType
import ai.run21.core.model.ToolRequest
import ai.run21.core.execution.ToolImplementation
import java.math.BigDecimal
import java.math.RoundingMode

class BigDecimalRoundingToolImplementation : ToolImplementation {
    override val descriptor = ToolImplementationDescriptor(
        implementationCode = ImplementationCode("big-decimal-rounding-v1"),
        toolCode = ToolCode("accounting.round-values"),
        type = ToolImplementationType.JVM_CLASS,
        version = "1.0.0",
        status = CapabilityStatus.DONE,
        runtimeMetadata = mapOf(
            "deterministic" to true,
            "defaultRoundingMode" to RoundingMode.HALF_UP.name,
        ),
    )

    override suspend fun execute(request: ToolRequest): ToolExecutionOutput {
        val scale = request.inputPayload["scale"] as Int
        val roundingMode = RoundingMode.valueOf(request.inputPayload["roundingMode"] as String)

        @Suppress("UNCHECKED_CAST")
        val values = request.inputPayload["values"] as Map<String, BigDecimal>

        val roundedValues = values.mapValues { (_, value) ->
            value.setScale(scale, roundingMode)
        }

        return ToolExecutionOutput(
            outputPayload = mapOf(
                "roundedValues" to roundedValues,
                "scale" to scale,
                "roundingMode" to roundingMode.name,
            ),
            metadata = mapOf(
                "inputValueCount" to values.size,
                "implementation" to descriptor.implementationCode.value,
            ),
        )
    }
}
```

### Wiring

Runtime modules normally perform this wiring once during application startup. The `core` module keeps it explicit and framework-independent.

```kotlin
import ai.run21.core.CoreDefaults
import ai.run21.core.execution.DefaultToolExecutionService
import ai.run21.core.execution.DefaultToolExecutor
import ai.run21.core.model.CapabilityStatus
import ai.run21.core.model.ContractSchema
import ai.run21.core.model.ToolCode
import ai.run21.core.model.ToolDescriptor
import ai.run21.core.registry.InMemoryToolExecutorRegistry
import ai.run21.core.registry.InMemoryToolImplementationRegistry
import ai.run21.core.registry.InMemoryToolRegistry

val toolCode = ToolCode("accounting.round-values")

val toolDescriptor = ToolDescriptor(
    code = toolCode,
    name = "Accounting Value Rounding",
    description = "Rounds a fixed set of accounting values to the requested decimal scale.",
    status = CapabilityStatus.DONE,
    inputSchema = ContractSchema(
        mapOf(
            "type" to "object",
            "required" to listOf("values", "scale", "roundingMode"),
            "properties" to mapOf(
                "values" to mapOf(
                    "type" to "object",
                    "required" to listOf("grossAmount", "taxBase", "taxAmount", "netAmount"),
                ),
                "scale" to mapOf("type" to "integer"),
                "roundingMode" to mapOf("type" to "string"),
            ),
        ),
    ),
    outputSchema = ContractSchema(
        mapOf(
            "type" to "object",
            "required" to listOf("roundedValues", "scale", "roundingMode"),
        ),
    ),
)

val implementation = BigDecimalRoundingToolImplementation()
val implementationRegistry = InMemoryToolImplementationRegistry(listOf(implementation))
val executor = DefaultToolExecutor(
    descriptor = toolDescriptor,
    implementationRegistry = implementationRegistry,
)

val service = DefaultToolExecutionService(
    dispatcherChain = CoreDefaults.structuredDirectDispatcherChain(),
    toolRegistry = InMemoryToolRegistry(listOf(toolDescriptor)),
    executorRegistry = InMemoryToolExecutorRegistry(listOf(executor)),
)
```

### Direct call through the builder API

```kotlin
import ai.run21.core.execution.ToolCalls
import ai.run21.core.model.ActorType
import ai.run21.core.model.ExecutionSourceType
import java.math.BigDecimal
import java.math.RoundingMode

val result = ToolCalls.execute(service) {
    tool("accounting.round-values")
    implementation("big-decimal-rounding-v1")

    actor(
        id = "user.evgenii",
        type = ActorType.HUMAN,
        displayName = "Evgenii",
    )

    source(
        type = ExecutionSourceType.CLI,
        name = "run21-local-cli",
        metadata = mapOf(
            "command" to "tool execute accounting.round-values",
        ),
    )

    input(
        "values",
        mapOf(
            "grossAmount" to BigDecimal("125430.9876"),
            "taxBase" to BigDecimal("104525.8234"),
            "taxAmount" to BigDecimal("13588.3569"),
            "netAmount" to BigDecimal("111842.6307"),
        ),
    )
    input("scale", 2)
    input("roundingMode", RoundingMode.HALF_UP.name)

    version(
        definitionVersion = "round-values-schema-v1",
        implementationVersion = "1.0.0",
        metadata = mapOf(
            "roundingPolicy" to "accounting-half-up-v1",
        ),
    )

    correlationId("example-rounding-001")

    metadata("companyCode", "demo-company")
    metadata("period", "2026-05")
}
```

The business output is available in `result.outputPayload`:

```kotlin
mapOf(
    "roundedValues" to mapOf(
        "grossAmount" to BigDecimal("125430.99"),
        "taxBase" to BigDecimal("104525.82"),
        "taxAmount" to BigDecimal("13588.36"),
        "netAmount" to BigDecimal("111842.63"),
    ),
    "scale" to 2,
    "roundingMode" to "HALF_UP",
)
```

The final `ToolResult` also contains execution status, structured errors when execution fails, dispatcher trace, actor/source metadata, implementation snapshot, version snapshot, and correlation metadata.


## Example: Mock LLM Dispatcher Calling the Same Tool

The same rounding tool can also be invoked from raw prompt-like input. In this example, the dispatcher does not call a real model. It simulates an LLM response by converting a prompt into the same `ToolRequest` that the direct builder API would create.

```text
ToolCalls.dispatch(...)
  -> RawDispatchInput
  -> MockLlmRoundingDispatcher
  -> ToolRequest
  -> DefaultToolExecutionService
  -> DefaultToolExecutor
  -> BigDecimalRoundingToolImplementation
  -> ToolResult
```

The tool implementation and service wiring are the same as in the direct execution example. Only the dispatcher chain changes.

### Mock LLM dispatcher

```kotlin
import ai.run21.core.dispatch.Dispatcher
import ai.run21.core.model.DispatchAccepted
import ai.run21.core.model.DispatchContext
import ai.run21.core.model.DispatchInput
import ai.run21.core.model.DispatchOutcomeType
import ai.run21.core.model.DispatcherCode
import ai.run21.core.model.DispatcherTraceStep
import ai.run21.core.model.ImplementationCode
import ai.run21.core.model.RawDispatchInput
import ai.run21.core.model.ToolCode
import ai.run21.core.model.ToolRequest
import java.math.BigDecimal
import java.math.RoundingMode

class MockLlmRoundingDispatcher(
    private val toolCode: ToolCode,
    private val implementationCode: ImplementationCode,
) : Dispatcher {
    override val code = DispatcherCode("mock-llm")

    override suspend fun dispatch(input: DispatchInput, context: DispatchContext): DispatchAccepted {
        val prompt = (input as? RawDispatchInput)
            ?.payload
            ?.get("prompt")
            ?.toString()
            .orEmpty()

        val request = ToolRequest(
            targetTool = toolCode,
            targetImplementation = implementationCode,
            inputPayload = mapOf(
                "values" to mapOf(
                    "grossAmount" to BigDecimal("125430.9876"),
                    "taxBase" to BigDecimal("104525.8234"),
                    "taxAmount" to BigDecimal("13588.3569"),
                    "netAmount" to BigDecimal("111842.6307"),
                ),
                "scale" to 2,
                "roundingMode" to RoundingMode.HALF_UP.name,
            ),
            actor = input.actor,
            source = input.source,
            correlation = input.correlation,
            metadata = mapOf(
                "dispatcherMode" to "mock-llm",
                "prompt" to prompt,
            ),
        )

        return DispatchAccepted(
            request = request,
            traceStep = DispatcherTraceStep(
                dispatcherCode = code,
                outcome = DispatchOutcomeType.ACCEPTED,
                confidence = 1.0,
                inputSummary = prompt,
                outputSummary = "Prepared accounting.round-values request.",
                correlationId = context.correlation.correlationId,
                metadata = mapOf(
                    "mock" to true,
                    "targetTool" to toolCode.value,
                    "targetImplementation" to implementationCode.value,
                ),
            ),
        )
    }
}
```

### Wiring with the mock dispatcher

```kotlin
import ai.run21.core.dispatch.OrderedDispatcherChain
import ai.run21.core.execution.DefaultToolExecutionService
import ai.run21.core.execution.DefaultToolExecutor
import ai.run21.core.model.ToolCode
import ai.run21.core.model.ImplementationCode
import ai.run21.core.registry.InMemoryToolExecutorRegistry
import ai.run21.core.registry.InMemoryToolImplementationRegistry
import ai.run21.core.registry.InMemoryToolRegistry

val toolCode = ToolCode("accounting.round-values")
val implementationCode = ImplementationCode("big-decimal-rounding-v1")

val implementation = BigDecimalRoundingToolImplementation()
val implementationRegistry = InMemoryToolImplementationRegistry(listOf(implementation))
val executor = DefaultToolExecutor(
    descriptor = toolDescriptor,
    implementationRegistry = implementationRegistry,
)

val service = DefaultToolExecutionService(
    dispatcherChain = OrderedDispatcherChain(
        listOf(
            MockLlmRoundingDispatcher(
                toolCode = toolCode,
                implementationCode = implementationCode,
            ),
        ),
    ),
    toolRegistry = InMemoryToolRegistry(listOf(toolDescriptor)),
    executorRegistry = InMemoryToolExecutorRegistry(listOf(executor)),
)
```

### Raw prompt invocation through `ToolCalls`

```kotlin
import ai.run21.core.execution.ToolCalls
import ai.run21.core.model.ActorType
import ai.run21.core.model.ExecutionSourceType

val result = ToolCalls.dispatch(service) {
    prompt("Round gross amount, tax base, tax amount, and net amount to 2 decimals using HALF_UP.")

    actor(
        id = "user.evgenii",
        type = ActorType.HUMAN,
        displayName = "Evgenii",
    )

    source(
        type = ExecutionSourceType.CLI,
        name = "run21-local-cli",
        metadata = mapOf(
            "command" to "tool dispatch accounting rounding prompt",
        ),
    )

    attribute("companyCode", "demo-company")
    attribute("period", "2026-05")
    correlationId("example-mock-llm-rounding-001")
}
```

The business output is the same as the direct call:

```kotlin
mapOf(
    "roundedValues" to mapOf(
        "grossAmount" to BigDecimal("125430.99"),
        "taxBase" to BigDecimal("104525.82"),
        "taxAmount" to BigDecimal("13588.36"),
        "netAmount" to BigDecimal("111842.63"),
    ),
    "scale" to 2,
    "roundingMode" to "HALF_UP",
)
```

The audit trace identifies `mock-llm` as the dispatcher that produced the `ToolRequest`. A real LLM dispatcher would replace the hardcoded parsing logic but keep the same boundary: it would produce a structured `ToolRequest`, and the registered tool implementation would still perform the deterministic work.

## Dispatcher Chain Example

A runtime module can add dispatchers before the pass-through dispatcher. For example, an LLM dispatcher can receive `RawDispatchInput`, extract the intended tool and arguments, and produce the same `ToolRequest` model used by direct execution.

```text
RawDispatchInput
  -> Custom or LLM dispatcher
  -> ToolRequest
  -> ToolExecutionService
  -> ToolExecutor
  -> ToolImplementation
  -> ToolResult
```

The direct path uses the same structure:

```text
StructuredDispatchInput
  -> PassThroughDispatcher
  -> ToolRequest
  -> ToolExecutionService
  -> ToolExecutor
  -> ToolImplementation
  -> ToolResult
```


## Formula Tool Module

The `:tools:formula` module contains the first concrete tool implementation outside `core`.
It registers `formula.evaluate`, a deterministic EvalEx-backed formula calculator that evaluates
an expression string with structured variables and returns a numeric `BigDecimal` result.

The tool still uses the standard execution path:

```text
ToolCalls.execute
  -> StructuredDispatchInput
  -> PassThroughDispatcher
  -> ToolRequest
  -> DefaultToolExecutionService
  -> DefaultToolExecutor
  -> EvalExFormulaToolImplementation
  -> ToolResult
```

Minimal structured call:

```kotlin
import ai.run21.core.execution.ToolCalls
import ai.run21.core.model.ActorType
import ai.run21.core.model.ExecutionSourceType
import ai.run21.tools.formula.FormulaTool
import ai.run21.tools.formula.FormulaToolWiring
import java.math.BigDecimal
import java.math.RoundingMode

val service = FormulaToolWiring.service()

val result = ToolCalls.execute(service) {
    tool(FormulaTool.TOOL_CODE)
    implementation(FormulaTool.IMPLEMENTATION_CODE)

    actor("user.evgenii", ActorType.HUMAN, "Evgenii")
    source(ExecutionSourceType.CLI, "run21-local-cli")

    input(FormulaTool.Input.EXPRESSION, "(grossAmount - discount) * taxRate")
    input(
        FormulaTool.Input.VARIABLES,
        mapOf(
            "grossAmount" to BigDecimal("125.00"),
            "discount" to BigDecimal("15.00"),
            "taxRate" to BigDecimal("0.125"),
        ),
    )
    input(FormulaTool.Input.SCALE, 2)
    input(FormulaTool.Input.ROUNDING_MODE, RoundingMode.HALF_UP.name)
}
```

The business output is available in `result.outputPayload["value"]`.

## Build

The `core` and `tools/formula` modules are Gradle subprojects. In a full project checkout, run from
the project root:

```bash
./gradlew :core:test
./gradlew :tools:formula:test
```
