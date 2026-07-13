# Run21AI

> **Transform human intent into deterministic tool execution.**

Run21AI is a lightweight Kotlin-first, dispatcher-driven tool execution engine.

It separates **request interpretation** from **tool execution** so that the same tool can be called in two ways:

- directly, with a complete structured request;
- indirectly, from raw or natural-language input that must first be validated, enriched, interpreted, or routed.

Both paths converge on the same canonical `ToolRequest` and continue through the same deterministic execution pipeline.

```text
Input
  -> DispatcherChain
  -> ToolRequest
  -> ToolExecutor
  -> ToolImplementation
  -> ToolResult
```

Tools perform real work. Dispatchers prepare requests.

An LLM may participate as a dispatcher, but it never replaces the tool implementation and never performs the final business operation.

---

## Table of Contents

- [The Problem](#the-problem)
- [A Real-World Example](#a-real-world-example)
- [Why Existing Approaches Are Not Enough](#why-existing-approaches-are-not-enough)
- [The Run21AI Approach](#the-run21ai-approach)
- [Design Principles](#design-principles)
- [What Run21AI Is and Is Not](#what-run21ai-is-and-is-not)
- [Execution Model](#execution-model)
- [Why Every Call Uses a DispatcherChain](#why-every-call-uses-a-dispatcherchain)
- [Two Ways to Enter the Same Pipeline](#two-ways-to-enter-the-same-pipeline)
- [Core Components](#core-components)
- [DispatcherChain Semantics](#dispatcherchain-semantics)
- [Tool Execution Semantics](#tool-execution-semantics)
- [Auditability and Reproducibility](#auditability-and-reproducibility)
- [Current Project Scope](#current-project-scope)
- [Quick Start](#quick-start)
- [Direct Kotlin Execution](#direct-kotlin-execution)
- [REST API](#rest-api)
- [Prompt Resolution with Ollama](#prompt-resolution-with-ollama)
- [Rust CLI Client](#rust-cli-client)
- [System Tests](#system-tests)
- [Project Structure](#project-structure)
- [Core Package Overview](#core-package-overview)
- [Adding a Tool](#adding-a-tool)
- [Adding a Dispatcher](#adding-a-dispatcher)
- [Error Model](#error-model)
- [Architecture Boundaries](#architecture-boundaries)
- [Testing Strategy](#testing-strategy)
- [Documentation](#documentation)
- [Build and Test](#build-and-test)
- [Roadmap Direction](#roadmap-direction)
- [FAQ](#faq)
- [Glossary](#glossary)

---

## The Problem

Every profession has tools.

Accountants use calculators and spreadsheets. Analysts use SQL and reporting systems. Test engineers use REST APIs and automation frameworks. Operations teams use CLIs and infrastructure APIs. Business applications expose increasingly capable services.

These tools are powerful because they are explicit and deterministic. They accept a defined input, perform a defined operation, and return a defined result.

But people usually do not think in the language of the tool.

A person thinks:

> “Calculate this employee's salary using the base rate, bonus, deductions, and the current payroll rules.”

A calculator expects a mathematical expression.

A spreadsheet expects cell references and formulas.

A REST API expects an endpoint, an HTTP method, and a JSON payload.

A command-line tool expects flags and arguments.

Humans naturally describe **what they want to achieve**. Software usually requires an exact description of **how to perform it**.

This creates a gap between:

```text
Human intent
    and
Deterministic software interfaces
```

Run21AI is designed to bridge that gap without giving up deterministic execution, stable contracts, validation, testing, or auditability.

---

## A Real-World Example

Consider a payroll-related capability.

A direct system-to-system caller may already know the exact formula and input values:

```text
expression = "(baseSalary + bonus) - deductions"
variables = {
  baseSalary: 4000,
  bonus: 600,
  deductions: 900
}
```

That caller should be able to execute the formula tool directly.

A human user may instead write:

> “Calculate the employee's salary from a base salary of 4000, add a 15% bonus, and subtract 900 in deductions.”

That input first needs interpretation:

- identify the intended tool;
- extract the required parameters;
- validate that required information is present;
- build a structured request;
- reject ambiguity when necessary.

After interpretation, the actual calculation should still be performed by the same deterministic tool implementation.

```text
Structured caller
       |
       | already knows the tool and payload
       v
StructuredDispatchInput
       |
       v
DispatcherChain
       |
       v
ToolRequest
       |
       v
ToolExecutor
       |
       v
Formula Tool
       |
       v
ToolResult


Human prompt
       |
       | requires interpretation
       v
RawDispatchInput
       |
       v
DispatcherChain
       |
       v
ToolRequest
       |
       v
ToolExecutor
       |
       v
Formula Tool
       |
       v
ToolResult
```

Different input forms. The same execution contract. The same tool. The same result model.

---

## Why Existing Approaches Are Not Enough

Run21AI is not intended to replace direct APIs or LLM applications. It addresses the architectural gap between them.

### Direct APIs are precise but require technical knowledge

A direct API is usually the best choice when the caller already knows:

- which capability to call;
- which parameters are required;
- how values must be encoded;
- which validation rules apply.

Direct calls are deterministic and easy to test, but they expose the technical contract directly to the caller.

### Unrestricted AI agents are flexible but can blur responsibilities

An LLM can understand natural-language intent, but an unrestricted agent may also:

- perform calculations itself;
- invent missing details;
- call external systems directly;
- bypass business validation;
- mix interpretation with execution;
- produce behavior that is difficult to reproduce or audit.

That may be acceptable for some applications, but it is a poor fit for operations where execution must remain controlled and testable.

### Run21AI keeps interpretation and execution separate

Run21AI uses dispatchers to prepare a structured `ToolRequest` and registered tools to perform real work.

```text
Flexible interpretation
          |
          v
    ToolRequest
          |
          v
Deterministic execution
```

The interpretation strategy can change without changing the tool implementation.

The tool implementation can change without changing how intent is captured.

---

## The Run21AI Approach

Run21AI defines one canonical execution model:

```text
Input
  -> DispatcherChain
  -> ToolRequest
  -> ToolExecutor
  -> ToolImplementation
  -> ToolResult
```

The model has one important boundary:

> A dispatcher may decide **what should be executed** and prepare the request. A tool implementation performs **the actual work**.

A dispatcher may:

- inspect input;
- validate input;
- normalize values;
- enrich context;
- classify intent;
- choose a target tool;
- choose a target implementation;
- build a `ToolRequest`;
- reject incomplete, ambiguous, invalid, or unsafe input.

A dispatcher must not:

- calculate the final business result;
- perform the target operation itself;
- bypass `ToolExecutor`;
- bypass audit metadata;
- directly replace a tool implementation.

A tool implementation may:

- evaluate a formula;
- process a document;
- execute domain logic;
- call an external service;
- work with a spreadsheet engine;
- perform any other registered capability.

The tool returns deterministic execution output, which is normalized into a `ToolResult` with status, error information, and audit metadata.

---

## Design Principles

### One execution pipeline

Structured calls and dispatcher-based calls do not create separate execution engines.

Every request passes through a `DispatcherChain` and eventually becomes a `ToolRequest`.

### One canonical request model

`ToolRequest` is the stable boundary between interpretation and execution.

Dispatchers, APIs, CLIs, tests, and runtime modules converge on the same request contract.

### Tools perform real work

Business operations belong in `ToolImplementation`, not in dispatchers.

### Dispatchers prepare requests

Dispatchers analyze, validate, enrich, reject, or route input.

They do not replace the target tool.

### LLMs are optional

Run21AI does not require an LLM.

A dispatcher can be implemented with:

- pass-through logic;
- deterministic rules;
- validation logic;
- custom parsing;
- an LLM;
- a combination of these approaches.

### LLMs are dispatchers, not executors

An LLM may produce a structured `ToolRequest` or reject ambiguous input.

The registered tool implementation still performs the operation.

### Deterministic execution remains testable

Tools and execution contracts can be tested without a model, network connection, or external runtime.

### The core remains framework-independent

Frameworks and provider integrations belong in runtime or adapter modules.

The `core` module defines execution semantics and contracts only.

### Every result is auditable

Execution records include actor, source, dispatcher trace, tool snapshot, implementation snapshot, correlation metadata, and optional version metadata.

---

## What Run21AI Is and Is Not

### Run21AI is

- a dispatcher-driven tool execution engine;
- a stable boundary between intent interpretation and deterministic execution;
- a Kotlin-first set of execution contracts;
- a framework-independent core with runtime adapters outside the core;
- a foundation for direct calls, rule-based dispatch, and LLM-assisted request preparation;
- an auditable execution model.

### Run21AI is not

- an autonomous AI agent framework;
- a general-purpose chatbot;
- a replacement for business logic;
- a system where an LLM is trusted to calculate final domain results;
- a workflow engine in its current scope;
- a separate execution engine for every tool type.

Formula evaluation is a tool. Spreadsheet processing is a tool. An HTTP operation is a tool. A document operation is a tool.

Run21AI does not create a separate execution path for “calculations.”

---

## Execution Model

The compact model is:

```text
Input
  -> DispatcherChain
  -> ToolRequest
  -> ToolExecutor
  -> ToolImplementation
  -> ToolResult
```

The runtime-level model is:

```text
External caller
  -> DispatchInput
  -> ToolExecutionService
  -> DispatcherChain
  -> ToolRequest
  -> ToolRegistry
  -> ToolExecutorRegistry
  -> ToolExecutor
  -> ToolImplementationRegistry
  -> ToolImplementation
  -> ToolExecutionOutput
  -> ToolResult
```

The additional runtime components perform resolution and composition, but they do not change the central model.

### Input

Input represents what entered the dispatcher chain.

The core currently defines two primary input forms:

- `StructuredDispatchInput` — already contains a complete `ToolRequest`;
- `RawDispatchInput` — contains raw or partially structured data that needs a dispatcher.

### DispatcherChain

`DispatcherChain` processes dispatchers in order until one accepts or rejects the request.

### ToolRequest

`ToolRequest` is the canonical execution request. It identifies:

- the target tool;
- an optional target implementation;
- input payload;
- actor;
- execution source;
- correlation metadata;
- optional version metadata;
- request metadata;
- dispatcher trace.

### ToolExecutor

`ToolExecutor` owns the standard execution protocol for a tool:

- verify the target tool;
- validate the request;
- resolve the implementation;
- handle missing implementations;
- invoke the implementation;
- build the final result;
- attach audit metadata.

### ToolImplementation

`ToolImplementation` performs the concrete operation.

### ToolResult

`ToolResult` is the canonical response. It contains:

- execution status;
- output payload on success;
- structured error on failure or rejection;
- audit metadata;
- result metadata.

---

## Why Every Call Uses a DispatcherChain

At first, a direct structured request may appear not to need dispatching.

Run21AI still sends it through a dispatcher chain for one reason: the system should have **one execution boundary**, not a special shortcut that bypasses normal semantics.

For direct execution, the chain can contain only `PassThroughDispatcher`:

```text
ToolCalls.execute(...)
  -> StructuredDispatchInput
  -> PassThroughDispatcher
  -> ToolRequest
  -> ToolExecutor
  -> ToolImplementation
  -> ToolResult
```

`PassThroughDispatcher` does not reinterpret or rewrite the request. It accepts an already prepared request and preserves the shared execution path.

This gives structured and raw execution the same:

- request model;
- validation boundary;
- executor contract;
- implementation contract;
- result model;
- error model;
- audit model.

The dispatcher boundary can later support runtime-specific behavior without creating a competing execution path.

---

## Two Ways to Enter the Same Pipeline

### Structured execution

Use structured execution when the caller already knows the target tool and payload.

```text
ToolCalls.execute
  -> StructuredDispatchInput
  -> PassThroughDispatcher
  -> ToolRequest
  -> ToolExecutor
  -> ToolImplementation
  -> ToolResult
```

Typical callers:

- REST endpoints;
- CLI commands;
- tests;
- workers;
- other backend services;
- scheduled jobs.

### Dispatcher-based execution

Use dispatcher-based execution when the input must be interpreted, enriched, validated, or routed.

```text
ToolCalls.dispatch
  -> RawDispatchInput
  -> DispatcherChain
  -> ToolRequest
  -> ToolExecutor
  -> ToolImplementation
  -> ToolResult
```

Typical dispatchers:

- rule-based dispatcher;
- validation dispatcher;
- enrichment dispatcher;
- custom domain parser;
- LLM dispatcher.

### Different inputs. Same execution.

```text
Structured request ------------------+
                                     |
                                     v
                              DispatcherChain
                                     |
                                     v
                                ToolRequest
                                     |
                                     v
                                ToolExecutor
                                     |
                                     v
                           ToolImplementation
                                     |
                                     v
                                ToolResult
                                     ^
                                     |
Raw or natural-language input -------+
```

---

## Core Components

### `DispatchInput`

External input accepted by the dispatcher chain.

### `StructuredDispatchInput`

Input that already contains a complete `ToolRequest`.

### `RawDispatchInput`

Unstructured or partially structured input that requires a dispatcher to prepare a `ToolRequest`.

### `Dispatcher`

One step in the request preparation chain.

A dispatcher returns one of four outcomes:

- accepted;
- enriched;
- rejected;
- passed.

### `DispatcherChain`

Runs dispatchers in order and records trace metadata.

### `ToolRequest`

The canonical request consumed by the execution layer.

### `ToolDescriptor`

Describes a tool capability, including code, name, description, schemas, and status.

### `ToolExecutor`

Owns tool-level validation, implementation resolution, invocation, error normalization, and audit creation.

### `ToolImplementation`

Performs the actual operation.

### `ToolExecutionOutput`

Raw output returned by an implementation before it is wrapped into the canonical result.

### `ToolResult`

The canonical execution response.

### Registries

The core defines registries for:

- tool descriptors;
- tool executors;
- tool implementations;
- named dispatcher chains.

The core provides lightweight in-memory implementations for tests and simple runtime composition.

### `ToolExecutionService`

The main execution entry point.

It dispatches input, resolves the target tool and executor, and returns a `ToolResult`.

### `ToolCalls`

The recommended compact API for application code, examples, and high-level tests.

Use:

- `ToolCalls.execute(...)` for structured calls;
- `ToolCalls.dispatch(...)` for raw calls.

---

## DispatcherChain Semantics

A dispatcher receives the current `DispatchInput` and returns a structured outcome.

### Accepted

The dispatcher produced a `ToolRequest`.

The chain stops, validates the request, and returns it to the execution layer.

```text
Input
  -> Dispatcher
  -> DispatchAccepted(ToolRequest)
  -> execution
```

### Enriched

The dispatcher produced an updated input.

The chain continues with the enriched input.

```text
Input
  -> MetadataEnricher
  -> DispatchEnriched(updated input)
  -> next dispatcher
```

### Rejected

The dispatcher decided that execution must not continue.

The chain stops and returns a structured error.

```text
Input
  -> ValidationDispatcher
  -> DispatchRejected(ToolError)
  -> ToolResult(status = rejected)
```

### Passed

The dispatcher did not handle the input.

The next dispatcher receives the same current input.

```text
Input
  -> RuleDispatcher
  -> DispatchPassed
  -> LlmDispatcher
```

### Ordered processing

Dispatchers run in configured order.

```text
RawDispatchInput
  -> RuleBasedDispatcher
  -> ContextEnrichmentDispatcher
  -> LlmDispatcher
  -> ToolRequest
```

The first accepted or rejected outcome is terminal.

### Trace recording

Each dispatcher step can record:

- dispatcher code;
- outcome;
- confidence;
- input summary;
- output summary;
- correlation ID;
- error;
- metadata.

The accumulated trace becomes part of the execution audit record.

---

## Tool Execution Semantics

Once dispatching produces a valid `ToolRequest`, execution follows a deterministic path.

```text
ToolRequest
  -> ToolRegistry
  -> ToolExecutorRegistry
  -> ToolExecutor
  -> ToolImplementationRegistry
  -> ToolImplementation
  -> ToolExecutionOutput
  -> ToolResult
```

### Tool resolution

The execution service resolves the target `ToolDescriptor` from `ToolRegistry`.

Unknown tools return a structured error.

### Executor resolution

The execution service resolves a `ToolExecutor` by tool code.

Known tools without an executor use a safe unsupported executor and return a structured error.

### Implementation resolution

The executor resolves the requested implementation.

A request may explicitly name an implementation. If it does not, a runtime registry may apply a default selection rule.

### Validation

Core-level validation protects common invariants.

Tool-specific validation belongs to the concrete tool executor or implementation.

### Invocation

The implementation receives the canonical request and performs the operation.

### Result creation

Implementation output is wrapped into `ToolResult` with:

- status;
- output or structured error;
- audit metadata;
- implementation metadata.

---

## Auditability and Reproducibility

Run21AI treats audit information as part of the result contract, not as an optional logging concern.

A `ToolResult` includes `AuditMetadata` describing:

- who initiated the request;
- where the request came from;
- how dispatching handled it;
- which tool was selected;
- which implementation was selected;
- which version metadata was supplied;
- which execution and correlation IDs were used;
- when execution started and completed.

The audit record stores snapshots of tool and implementation metadata so that later registry changes do not rewrite the meaning of an earlier result.

This supports:

- debugging;
- traceability;
- reproducibility;
- contract testing;
- operational analysis;
- future persistence and compliance use cases.

---

## Current Project Scope

Run21AI currently includes the following implementation slices.

### `:core`

Framework-independent execution contracts and default implementations:

- canonical models;
- dispatcher contracts;
- ordered dispatcher chain;
- pass-through dispatcher;
- tool request validation;
- execution service;
- default tool executor;
- registries;
- unsupported defaults;
- result factories;
- audit metadata;
- `ToolCalls` builders;
- deterministic contract tests.

### `:tools:formula`

The first concrete production tool.

It registers:

```text
tool code:           formula.evaluate
implementation code: formula-evalex-v1
```

The tool uses EvalEx to evaluate a formula with structured variables and returns a `BigDecimal` result.

The formula engine remains outside `core`.

### `:runtime:local`

A lightweight composition module that wires:

- core services;
- in-memory registries;
- concrete tools;
- executors;
- implementations;
- dispatcher chain profiles.

The current factory provides a formula-only runtime for structured execution.

### `:runtime:rest-api`

A Ktor-based HTTP runtime outside `core`.

It exposes health, readiness, documentation, tool discovery, request resolution, and execution endpoints.

### `:dispatchers:llm`

Provider-neutral LLM dispatcher behavior.

It depends on an injected `LlmDispatchClient` and does not contain provider-specific HTTP code.

### `:dispatchers:llm-ollama`

An Ollama adapter for the provider-neutral LLM dispatcher.

It uses Ollama-compatible structured output and keeps provider-specific code outside `core`.

### `clients/run21ctl`

An independent Rust CLI client for the REST API.

It is not part of the Gradle build.

### `tests/system`

Pytest-based black-box contract tests for the REST API and Rust CLI.

---

## Quick Start

The fastest current end-to-end path is the REST API with the formula tool.

### Start the server

From the project root:

```bash
./gradlew :runtime:rest-api:run
```

The server listens on:

```text
http://127.0.0.1:8080
```

### Check health

```bash
curl http://127.0.0.1:8080/health
```

### Execute the formula tool

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
      "id": "quick-start-user",
      "type": "human",
      "displayName": "Quick Start User"
    },
    "source": {
      "type": "api",
      "name": "curl"
    },
    "correlation": {
      "correlationId": "quick-start"
    }
  }'
```

The execution still follows the standard path:

```text
HTTP request
  -> StructuredDispatchInput
  -> PassThroughDispatcher
  -> ToolRequest
  -> DefaultToolExecutionService
  -> DefaultToolExecutor
  -> EvalExFormulaToolImplementation
  -> ToolResult
```

### Open API documentation

```text
http://127.0.0.1:8080/docs
```

The raw OpenAPI contract is available at:

```text
http://127.0.0.1:8080/openapi.yaml
```

---

## Direct Kotlin Execution

### Through the local runtime

The local runtime is the simplest composition boundary for application code.

```kotlin
import ai.run21.core.model.ActorType
import ai.run21.core.model.ExecutionSourceType
import ai.run21.runtime.local.LocalRun21RuntimeFactory
import ai.run21.tools.formula.FormulaTool
import java.math.BigDecimal

val runtime = LocalRun21RuntimeFactory.formulaOnly()

val result = runtime.execute {
    tool(FormulaTool.TOOL_CODE)
    implementation(FormulaTool.IMPLEMENTATION_CODE)

    actor(
        id = "user-1",
        type = ActorType.HUMAN,
        displayName = "User",
    )

    source(
        type = ExecutionSourceType.TEST,
        name = "local-runtime-example",
    )

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

The business value is available in:

```kotlin
result.outputPayload["value"]
```

### Through `ToolCalls`

`ToolCalls` is the recommended high-level API when working directly with a `ToolExecutionService`.

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

    actor(
        id = "user.evgenii",
        type = ActorType.HUMAN,
        displayName = "Evgenii",
    )

    source(
        type = ExecutionSourceType.CLI,
        name = "run21-local-cli",
    )

    input(
        FormulaTool.Input.EXPRESSION,
        "(grossAmount - discount) * taxRate",
    )

    input(
        FormulaTool.Input.VARIABLES,
        mapOf(
            "grossAmount" to BigDecimal("125.00"),
            "discount" to BigDecimal("15.00"),
            "taxRate" to BigDecimal("0.125"),
        ),
    )

    input(FormulaTool.Input.SCALE, 2)
    input(
        FormulaTool.Input.ROUNDING_MODE,
        RoundingMode.HALF_UP.name,
    )

    correlationId("formula-example")
}
```

This is a structured call, but it still passes through `PassThroughDispatcher` before execution.

---

## REST API

The REST API runtime keeps Ktor, HTTP DTOs, JSON mapping, routing, and OpenAPI concerns outside `core`.

### Start the server

```bash
./gradlew :runtime:rest-api:run
```

Override the default host or port:

```bash
RUN21_REST_HOST=127.0.0.1 \
RUN21_REST_PORT=9090 \
./gradlew :runtime:rest-api:run
```

### Endpoints

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

### Health and readiness

```bash
curl http://127.0.0.1:8080/health
curl http://127.0.0.1:8080/ready
```

### List tools

```bash
curl http://127.0.0.1:8080/v1/tools
```

### Read one descriptor

```bash
curl http://127.0.0.1:8080/v1/tools/formula.evaluate
```

### Resolve without executing

`POST /v1/tool-requests/resolve` runs only the dispatcher chain.

For structured input, it returns the prepared `ToolRequest` without invoking the tool.

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

### Execute

`POST /v1/executions` accepts structured or prompt-like requests.

The currently selected runtime profile determines which dispatcher chains are available.

---

## Prompt Resolution with Ollama

Run21AI keeps LLM behavior outside `core` and models it as dispatcher behavior.

The provider-neutral path is:

```text
RawDispatchInput
  -> LlmDispatcher
  -> LlmDispatchClient
  -> LlmDispatchResponse
  -> ToolRequest or DispatchRejected
```

The Ollama adapter provides the client-side integration:

```text
RawDispatchInput
  -> LlmDispatcher
  -> OllamaLlmDispatchClient
  -> Ollama /api/chat
  -> structured dispatcher JSON
  -> validation and normalization
  -> ToolRequest or DispatchRejected
```

The model is asked to return dispatcher output only.

It may:

- select a tool;
- select an implementation;
- prepare input payload;
- enrich metadata;
- reject ambiguity.

It must not:

- execute the tool;
- calculate the final business result;
- bypass the registered execution pipeline.

### Configuration

```text
RUN21_LLM_BASE_URL=http://localhost:11434
RUN21_LLM_MODEL=<installed-ollama-model>
RUN21_LLM_TIMEOUT_MS=30000
RUN21_LLM_TEMPERATURE=0.0
```

`RUN21_LLM_MODEL` is required.

Run21AI intentionally does not choose a default model because available models differ between local and remote Ollama installations.

### Start Ollama

```bash
ollama run <installed-ollama-model>
```

### Start the REST runtime with the Ollama profile

```bash
RUN21_REST_PROFILE=ollama \
RUN21_LLM_BASE_URL=http://localhost:11434 \
RUN21_LLM_MODEL=<installed-ollama-model> \
./gradlew :runtime:rest-api:run
```

### Resolve a prompt

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

The endpoint resolves intent into a `ToolRequest` without executing the formula tool.

Execution can then continue through the normal executor and implementation path.

### Deterministic tests

Unit tests for the LLM modules use fake clients or fake transport.

They do not require:

- a running Ollama instance;
- a model download;
- network access;
- an API key.

---

## Rust CLI Client

`clients/run21ctl` is an independent Rust CLI client for the Run21AI REST API.

It is intentionally outside the Gradle build.

### Run tests

```bash
cargo test --manifest-path clients/run21ctl/Cargo.toml
```

### Check backend health

```bash
cargo run --manifest-path clients/run21ctl/Cargo.toml -- health
```

The default backend URL is:

```text
http://127.0.0.1:8080
```

Override it with:

- `--base-url`;
- `RUN21_BASE_URL`.

### Useful commands

```bash
cargo run --manifest-path clients/run21ctl/Cargo.toml -- ready
cargo run --manifest-path clients/run21ctl/Cargo.toml -- tools
cargo run --manifest-path clients/run21ctl/Cargo.toml -- tool formula.evaluate
```

### Execute a tool

```bash
cargo run --manifest-path clients/run21ctl/Cargo.toml -- execute \
  --tool-code formula.evaluate \
  --implementation-code formula-evalex-v1 \
  --input-json '{"expression":"a + b","variables":{"a":10,"b":11}}' \
  --actor-id manual-user \
  --correlation-id run21ctl-manual-test
```

See [clients/run21ctl/README.md](clients/run21ctl/README.md) for the complete command reference.

---

## System Tests

`tests/system` contains pytest-based external contract tests.

These tests execute Run21AI from outside the JVM process:

```text
pytest
  -> HTTP API or Rust CLI
  -> runtime:rest-api
  -> DispatcherChain
  -> ToolExecutor
  -> ToolImplementation
  -> ToolResult
```

They validate deployed behavior rather than internal implementation details.

### Create a virtual environment

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -e tests/system
```

### Run against a server you started manually

Terminal 1:

```bash
./gradlew :runtime:rest-api:run
```

Terminal 2:

```bash
python -m pytest \
  -c tests/system/pytest.ini \
  tests/system/tests
```

### Let pytest manage the server

```bash
RUN21_START_SERVER=true \
python -m pytest \
  -c tests/system/pytest.ini \
  tests/system/tests
```

### Custom port

```bash
RUN21_BASE_URL=http://localhost:9090 \
RUN21_START_SERVER=true \
RUN21_SERVER_CMD='./gradlew :runtime:rest-api:run' \
RUN21_REST_PORT=9090 \
python -m pytest \
  -c tests/system/pytest.ini \
  tests/system/tests
```

### Configuration

```text
RUN21_BASE_URL=http://localhost:8080
RUN21_CLI_BIN=./clients/run21ctl/target/debug/run21ctl
RUN21_START_SERVER=false
RUN21_SERVER_CMD=./gradlew :runtime:rest-api:run
```

The current suite covers:

- health and readiness;
- tool listing;
- descriptor schemas;
- successful structured formula execution;
- structured errors for invalid formula input;
- CLI smoke tests when `run21ctl` is built;
- prompt-resolution contracts, with profile-dependent scenarios marked `xfail` when no raw-input dispatcher is enabled.

See [tests/system/README.md](tests/system/README.md) for the complete workflow and test matrix.

---

## Project Structure

```text
run21ai/
├── core/
│   ├── src/main/kotlin/
│   └── src/test/kotlin/
│
├── tools/
│   └── formula/
│
├── dispatchers/
│   ├── llm/
│   └── llm-ollama/
│
├── runtime/
│   ├── local/
│   └── rest-api/
│
├── clients/
│   └── run21ctl/
│
├── tests/
│   └── system/
│
├── docs/
│   ├── 01-requirements/
│   ├── 02-core/
│   ├── 03-architecture/
│   ├── 04-data-model/
│   ├── 05-api/
│   ├── 06-tools/
│   ├── 07-llm/
│   ├── 08-testing/
│   └── 09-generation/
│
├── AGENTS.md
├── ROADMAP.md
├── README.md
└── settings.gradle.kts
```

The module structure follows one dependency rule:

```text
runtime / adapters / tools
            |
            v
           core
```

Dependencies point inward.

`core` does not depend on runtime modules, provider adapters, external tool engines, or clients.

---

## Core Package Overview

### `ai.run21.core`

Shared defaults for minimal core composition.

The main helper is:

```kotlin
CoreDefaults.structuredDirectDispatcherChain()
```

It creates a dispatcher chain containing only `PassThroughDispatcher`.

### `ai.run21.core.model`

Canonical shared models:

- identifiers;
- actors;
- execution sources;
- dispatch input;
- dispatcher outcomes;
- dispatcher trace;
- tool descriptors;
- implementation descriptors;
- tool requests;
- tool results;
- errors;
- schemas;
- statuses;
- audit metadata.

### `ai.run21.core.dispatch`

Dispatcher contracts and chain behavior:

- `Dispatcher`;
- `DispatcherChain`;
- `OrderedDispatcherChain`;
- `PassThroughDispatcher`;
- request validation;
- unsupported dispatcher behavior;
- named dispatcher-chain registry.

### `ai.run21.core.execution`

Execution contracts and defaults:

- `ToolExecutionService`;
- `DefaultToolExecutionService`;
- `ToolExecutor`;
- `DefaultToolExecutor`;
- `ToolImplementation`;
- unsupported tool execution;
- result factories;
- audit metadata factory;
- `ToolCalls`;
- structured and raw call builders.

### `ai.run21.core.registry`

Registry contracts and in-memory implementations:

- `ToolRegistry`;
- `ToolExecutorRegistry`;
- `ToolImplementationRegistry`.

### Core tests

Core tests validate contracts and behavior through deterministic test implementations.

High-level execution tests prefer `ToolCalls` so examples and tests exercise the recommended public API.

---

## Adding a Tool

A tool is a named capability with a contract and one or more implementations.

A typical tool slice contains:

1. a `ToolDescriptor`;
2. a `ToolImplementation`;
3. input and output contract definitions;
4. tool-specific validation;
5. runtime wiring;
6. deterministic tests.

### 1. Define a descriptor

```kotlin
val descriptor = ToolDescriptor(
    code = ToolCode("example.echo"),
    name = "Echo",
    description = "Returns the supplied value.",
    status = CapabilityStatus.DONE,
)
```

### 2. Implement the tool

```kotlin
class EchoToolImplementation : ToolImplementation {
    override val descriptor = ToolImplementationDescriptor(
        implementationCode = ImplementationCode("echo-jvm-v1"),
        toolCode = ToolCode("example.echo"),
        type = ToolImplementationType.JVM_CLASS,
        version = "1.0.0",
        status = CapabilityStatus.DONE,
    )

    override suspend fun execute(
        request: ToolRequest,
    ): ToolExecutionOutput =
        ToolExecutionOutput(
            outputPayload = mapOf(
                "value" to request.inputPayload["value"],
            ),
        )
}
```

### 3. Register the implementation

Register the descriptor, implementation, and executor during runtime composition.

Do not add framework-specific registration code to `core`.

### 4. Test through the common pipeline

Prefer `ToolCalls.execute(...)` for integration-style tool tests.

```kotlin
val result = ToolCalls.execute(service) {
    tool("example.echo")
    implementation("echo-jvm-v1")
    actor("test-user", ActorType.HUMAN)
    source(ExecutionSourceType.TEST, "echo-test")
    input("value", "hello")
}
```

A new tool must not introduce a parallel execution path.

---

## Adding a Dispatcher

A dispatcher prepares or rejects requests.

It must not perform the target tool operation.

### Example shape

```kotlin
class ExampleDispatcher : Dispatcher {
    override val code = DispatcherCode("example")

    override suspend fun dispatch(
        input: DispatchInput,
        context: DispatchContext,
    ): DispatcherOutcome {
        // Interpret or validate input here.
        TODO("Return accepted, enriched, rejected, or passed")
    }
}
```

A dispatcher may return:

```kotlin
DispatchAccepted(request)
DispatchEnriched(updatedInput)
DispatchRejected(error)
DispatchPassed()
```

### LLM dispatcher rule

An LLM dispatcher may:

- analyze raw input;
- select a tool;
- extract parameters;
- enrich metadata;
- reject ambiguity;
- produce a `ToolRequest`.

It must not:

- perform the formula calculation;
- generate the final business result;
- call the target external system directly;
- bypass tool validation;
- bypass audit metadata.

---

## Error Model

Run21AI returns structured errors instead of relying on uncontrolled runtime failures for normal execution problems.

Core error codes include:

```text
UNKNOWN_TOOL
UNSUPPORTED_TOOL
IMPLEMENTATION_NOT_REGISTERED
DISPATCHER_NOT_REGISTERED
INVALID_TOOL_REQUEST
DISPATCH_REJECTED
NO_DISPATCHER_ACCEPTED
```

Errors are returned inside the canonical result model.

### Rejected

Use `REJECTED` when execution stops during dispatching or request preparation.

Examples:

- ambiguous prompt;
- invalid dispatcher output;
- no dispatcher can handle the input;
- unsafe or incomplete request.

### Failed

Use `FAILED` when a tool path was selected but execution cannot complete.

Examples:

- unknown tool;
- unsupported executor;
- missing implementation;
- tool-specific validation failure;
- implementation failure normalized by the runtime.

### Succeeded

Use `SUCCEEDED` when execution produced a valid output payload.

Every final result still includes audit metadata.

---

## Architecture Boundaries

Run21AI follows pragmatic clean architecture.

### Core responsibilities

The `core` module defines:

- execution semantics;
- canonical models;
- dispatcher contracts;
- executor contracts;
- implementation contracts;
- registry contracts;
- errors and statuses;
- audit metadata;
- safe defaults;
- compact call APIs.

### Runtime responsibilities

Runtime modules:

- compose registries;
- register tools;
- register implementations;
- register executors;
- select dispatcher profiles;
- expose local, HTTP, worker, or other entry points.

### Adapter responsibilities

Provider adapters contain integration-specific behavior.

Examples:

- Ollama HTTP protocol;
- Ktor routing;
- JSON serialization;
- EvalEx formula evaluation;
- CLI argument parsing.

### Forbidden core dependencies

The `core` module must not depend on:

- Ktor;
- Spring;
- Quarkus;
- PostgreSQL;
- Ollama;
- EvalEx;
- CLI frameworks;
- persistence frameworks;
- external service SDKs.

Framework and provider dependencies stay outside `core`.

---

## Testing Strategy

Run21AI tests contracts and behavior at several levels.

### Core contract tests

Core tests validate:

- framework independence;
- dispatcher ordering;
- accepted, enriched, rejected, and passed outcomes;
- request validation;
- pass-through behavior;
- tool resolution;
- implementation selection;
- unsupported defaults;
- structured errors;
- audit metadata;
- `ToolCalls` builders.

### Tool tests

Concrete tool modules test:

- tool-specific input validation;
- deterministic execution;
- output contracts;
- implementation metadata;
- execution through the common pipeline.

### Dispatcher tests

Dispatcher modules use deterministic fakes.

LLM dispatcher tests should not depend on a real model.

Provider adapters should test transport and mapping separately from core dispatcher behavior.

### REST runtime tests

Runtime tests validate:

- HTTP mapping;
- endpoint contracts;
- JSON serialization;
- OpenAPI alignment;
- execution and error responses.

### System tests

Pytest tests validate the deployed path from outside the JVM process.

### Recommended API in tests

Use:

```kotlin
ToolCalls.execute(...)
```

for structured execution tests.

Use:

```kotlin
ToolCalls.dispatch(...)
```

for raw input that must pass through a dispatcher chain.

Use lower-level models directly only when testing lower-level contracts such as dispatchers, registries, validation, unsupported defaults, or builders.

---

## Documentation

Project documentation is organized by concern.

### Navigation

- [Documentation Index](docs/INDEX.md)
- [Roadmap](ROADMAP.md)
- [Glossary](docs/00-glossary.md)

### Requirements

- [Requirements Index](docs/01-requirements/README.md)
- [Core Contract](docs/01-requirements/REQ-001-core-contract.md)
- [Tool Execution](docs/01-requirements/REQ-002-tool-execution.md)
- [Dispatcher Chain](docs/01-requirements/REQ-003-dispatcher-chain.md)
- [Audit and Versioning](docs/01-requirements/REQ-004-versioning-audit.md)

### Core

- [Core Documentation](docs/02-core/README.md)
- [Semantic Core](docs/02-core/semantic-core.md)
- [Execution Model](docs/02-core/execution-model.md)
- [Dispatcher Chain](docs/02-core/dispatcher-chain.md)
- [Tool Contracts](docs/02-core/tool-contracts.md)

### Architecture

- [Architecture Index](docs/03-architecture/README.md)
- [Architecture Overview](docs/03-architecture/architecture-overview.md)
- [Runtime View](docs/03-architecture/runtime-view.md)
- [Deployment View](docs/03-architecture/deployment-view.md)
- [Quality Attributes](docs/03-architecture/quality-attributes.md)
- [Risks and Technical Debt](docs/03-architecture/risks-and-technical-debt.md)
- [ADRs](docs/03-architecture/adr/README.md)
- [C4 Diagrams](docs/03-architecture/c4/README.md)

### API

- [API Documentation](docs/05-api/README.md)
- [API Overview](docs/05-api/api-overview.md)
- [OpenAPI Contract](docs/05-api/openapi.yaml)

### Tools

- [Tool Documentation](docs/06-tools/README.md)
- [Tool Contract](docs/06-tools/tool-contract.md)
- [Formula Tool](docs/06-tools/formula-tool.md)
- [Custom Tool](docs/06-tools/custom-tool.md)
- [Spreadsheet Tool](docs/06-tools/spreadsheet-tool.md)

### LLM

- [LLM Documentation](docs/07-llm/README.md)
- [LLM Dispatcher](docs/07-llm/llm-dispatcher.md)
- [Structured Output Schema](docs/07-llm/structured-output-schema.json)

### Testing

- [Testing Documentation](docs/08-testing/README.md)
- [Testing Strategy](docs/08-testing/testing-strategy.md)
- [Requirement-to-Test Matrix](docs/08-testing/requirement-test-matrix.md)

### Generation

- [Generation Documentation](docs/09-generation/README.md)
- [Global Generation Rules](docs/09-generation/global-code-generation-rules.md)
- [Core Generation Pack](docs/09-generation/core-generation-pack.md)
- [Specification Compliance Checklist](docs/09-generation/spec-compliance-checklist.md)

---

## Build and Test

Run commands from the project root.

### Core tests

```bash
./gradlew :core:test
```

### Formula tool tests

```bash
./gradlew :tools:formula:test
```

### LLM dispatcher tests

```bash
./gradlew :dispatchers:llm:test
```

### Ollama adapter tests

```bash
./gradlew :dispatchers:llm-ollama:test
```

These tests use deterministic fakes and do not require a running Ollama server.

### Local runtime tests

```bash
./gradlew :runtime:local:test
```

### REST runtime tests

```bash
./gradlew :runtime:rest-api:test
```

### Run the REST server

```bash
./gradlew :runtime:rest-api:run
```

### Rust CLI tests

```bash
cargo test --manifest-path clients/run21ctl/Cargo.toml
```

### System tests

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -e tests/system

RUN21_START_SERVER=true \
python -m pytest \
  -c tests/system/pytest.ini \
  tests/system/tests
```

---

## Roadmap Direction

Run21AI is evolving through small, contract-first vertical slices.

Current implemented direction includes:

- framework-independent core;
- dispatcher-driven execution;
- structured and raw input boundaries;
- deterministic formula tool;
- local runtime composition;
- Ktor REST API;
- provider-neutral LLM dispatcher;
- Ollama adapter;
- Rust CLI client;
- pytest system tests.

Future directions may include:

- additional deterministic tools;
- stronger schema validation;
- additional LLM provider adapters;
- persistent registries;
- richer runtime profiles;
- tool composition and workflow execution;
- improved observability;
- production deployment profiles.

The current execution model remains intentionally small:

```text
one input
  -> one resolved ToolRequest
  -> one tool execution
  -> one ToolResult
```

Workflow composition should build on this model rather than create a competing execution path.

See [ROADMAP.md](ROADMAP.md) for the staged plan.

---

## FAQ

### Is Run21AI an AI agent framework?

No.

Run21AI is a tool execution engine with dispatcher-based request preparation.

An LLM can be used as a dispatcher, but the LLM is not the execution engine.

### Does Run21AI require an LLM?

No.

Structured calls can use `PassThroughDispatcher`.

Raw calls can use rule-based, custom, validation, enrichment, or LLM dispatchers.

### Why not let the LLM execute the operation directly?

Because request interpretation and business execution have different requirements.

Interpretation benefits from flexibility.

Execution often requires deterministic logic, stable validation, testability, predictable errors, and auditability.

Run21AI keeps those responsibilities separate.

### Why does a direct call pass through DispatcherChain?

To preserve one execution boundary.

A pass-through dispatcher accepts the existing `ToolRequest` without semantic changes.

Direct execution is not a second execution engine.

### Can a dispatcher call an external service?

A dispatcher should not perform the target business operation.

Provider-specific services used only to interpret input, such as an LLM provider, may be used by a dispatcher adapter.

External business operations should be implemented as tools.

### Can a tool call an external API?

Yes.

An HTTP-backed capability can be implemented as a `ToolImplementation` outside `core`.

It should still use the common request, executor, result, error, and audit contracts.

### Is formula evaluation a special execution mode?

No.

Formula evaluation is simply one tool family.

It follows the same execution path as every other tool.

### Can one tool have multiple implementations?

Yes.

`ToolImplementationRegistry` supports multiple implementations for a tool.

A request may target a specific implementation, or a runtime registry may choose a default.

### Is `core` tied to Ktor or Ollama?

No.

Ktor belongs in the REST runtime. Ollama belongs in the provider adapter. EvalEx belongs in the formula tool module.

The core remains framework-independent.

### How should new behavior be tested?

Test contracts and behavior through the highest useful API.

Use `ToolCalls.execute(...)` for structured execution and `ToolCalls.dispatch(...)` for raw dispatcher-based execution.

Use lower-level models only for lower-level contract tests.

### Does Run21AI already support multi-step workflows?

Not as a separate implemented workflow engine in the current scope.

The current model resolves one request and executes one tool.

Future workflow composition can build on the same tool execution contract.

---

## Glossary

### Actor

A human, agent, or system identity that initiated a request.

### Input

Raw or structured data submitted to the dispatcher chain.

### Dispatcher

A component that analyzes, validates, enriches, routes, rejects, or passes input.

### DispatcherChain

An ordered sequence of dispatchers that produces a `ToolRequest` or a structured rejection.

### Pass-through dispatcher

A dispatcher that accepts already structured input without semantic interpretation.

### LLM dispatcher

A dispatcher that uses an LLM to convert raw input into a structured `ToolRequest` or rejection.

### Tool

A named executable capability with an input and output contract.

### ToolRequest

The canonical structured request passed from dispatching to execution.

### ToolExecutor

A component that validates the request, resolves an implementation, invokes it, and builds the final result.

### ToolImplementation

A concrete mechanism that performs real work.

### ToolResult

The canonical execution result containing status, output or error, metadata, and audit information.

### AuditMetadata

An immutable execution snapshot describing actor, source, dispatch trace, tool, implementation, version, correlation, and timing information.

---

## Summary

Run21AI is built around one idea:

> **Interpret intent flexibly. Execute tools deterministically.**

```text
Human or application input
           |
           v
    DispatcherChain
           |
           v
      ToolRequest
           |
           v
      ToolExecutor
           |
           v
  ToolImplementation
           |
           v
       ToolResult
```

The dispatcher prepares the request.

The tool performs the work.

The execution pipeline remains stable, explicit, testable, and auditable.
