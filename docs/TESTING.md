# Testing

Run commands from the repository root.

## Gradle Tests

Run every Kotlin module test:

```bash
./gradlew test
```

Focused commands:

```bash
./gradlew :core:test
./gradlew :tools:formula:test
./gradlew :dispatchers:llm:test
./gradlew :dispatchers:llm-ollama:test
./gradlew :runtime:local:test
./gradlew :runtime:rest-api:test
```

## Test Layers

| Layer | Location | Responsibility |
|---|---|---|
| Core contract | `core/src/test/kotlin` | Framework independence, model invariants, dispatcher semantics, registries, execution, errors, and audit metadata |
| Tool integration | `tools/*/src/test/kotlin` | Tool-specific validation and deterministic behavior through core contracts |
| Dispatcher adapter | `dispatchers/*/src/test/kotlin` | Provider-neutral mapping and provider-specific prompt/schema/transport behavior |
| Runtime/API | `runtime/*/src/test/kotlin` | Composition, HTTP mapping, endpoints, and runtime profiles |
| Client | `clients/run21ctl/tests` | Rust command parsing and REST request construction |
| System | `tests/system/tests` | Black-box HTTP and CLI behavior against a running backend |

Tests are the executable source of truth for verified behavior. Stable identifiers such as `TEST-CORE-*`, `TEST-EXEC-*`, `TEST-DISP-*`, and `TEST-AUDIT-*` may be used in test names, but a separate requirement matrix is not maintained.

## Rust CLI Tests

```bash
cargo test --manifest-path clients/run21ctl/Cargo.toml
```

## System-Test Installation

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -e tests/system
```

## Run Against an Existing Backend

Start the backend in one terminal:

```bash
./gradlew :runtime:rest-api:run
```

Run pytest in another terminal:

```bash
python -m pytest -c tests/system/pytest.ini tests/system/tests
```

## Let Pytest Manage the Backend

```bash
RUN21_START_SERVER=true \
python -m pytest -c tests/system/pytest.ini tests/system/tests
```

## System-Test Configuration

| Variable | Default | Purpose |
|---|---|---|
| `RUN21_BASE_URL` | `http://localhost:8080` | Backend URL used by HTTP tests and the CLI wrapper |
| `RUN21_CLI_BIN` | `./clients/run21ctl/target/debug/run21ctl` | Rust CLI binary for smoke tests |
| `RUN21_START_SERVER` | `false` | Let pytest start and stop the backend |
| `RUN21_SERVER_CMD` | `./gradlew :runtime:rest-api:run` | Backend command used by managed startup |

When using a custom port, set both the runtime port and test base URL:

```bash
RUN21_REST_PORT=9090 \
RUN21_BASE_URL=http://localhost:9090 \
RUN21_START_SERVER=true \
python -m pytest -c tests/system/pytest.ini tests/system/tests
```

Build the CLI before running CLI smoke tests:

```bash
cargo build --manifest-path clients/run21ctl/Cargo.toml
python -m pytest -c tests/system/pytest.ini tests/system/tests/test_cli_smoke.py
```

CLI tests are skipped when the configured binary does not exist.

## External LLM Tests

Deterministic module tests use fake clients and do not require Ollama. Tests against a real model must be optional, explicitly enabled, and separated from the default suite because model availability and output can vary.

## Validation Expectations

A public behavior change should include:

- a focused unit or contract test;
- an integration test when module wiring changes;
- REST and OpenAPI updates when the HTTP contract changes;
- system coverage when an externally observable workflow changes;
- an update to the single owning documentation file.
