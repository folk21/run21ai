# Run21AI System Tests

This package contains pytest-based system tests for Run21AI. The tests call the REST API, and some
tests call the Rust CLI, from outside the JVM process:

```text
pytest
  -> HTTP API / Rust CLI
  -> runtime:rest-api
  -> DispatcherChain
  -> ToolExecutor
  -> ToolImplementation
  -> ToolResult
```

The tests are independent from Gradle test tasks. They always require a real REST API process, but
pytest can either connect to a process that you started manually or start one for the test session.

Run the commands in this document from the project root unless a section explicitly says otherwise.
The pytest configuration lives in `tests/system/pytest.ini`; examples pass it explicitly with `-c`
so they work without changing into `tests/system`.

## Install

From the repository root, create a Python virtual environment and install the system-test package:

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -e tests/system
```

The editable install makes the helper package under `tests/system/src` available to pytest.

## Running Tests

### Option 1: Start the backend manually

Start the REST API in one terminal:

```bash
./gradlew :runtime:rest-api:run
```

Run pytest from the repository root in another terminal:

```bash
python -m pytest -c tests/system/pytest.ini tests/system/tests
```

This is the default mode. With `RUN21_START_SERVER=false`, pytest does not launch the backend. It
creates an HTTP client, waits for `/ready` at `RUN21_BASE_URL`, and then runs the tests.

### Option 2: Let pytest start the backend

Run:

```bash
RUN21_START_SERVER=true \
python -m pytest -c tests/system/pytest.ini tests/system/tests
```

In this mode the session fixture starts `RUN21_SERVER_CMD` before the first API test, waits until
`GET /ready` returns HTTP 200, and terminates the process at the end of the pytest session.

This is useful for local one-command runs and CI jobs where no backend is already running.

### Custom port

The test client and the backend command must agree on the same port.

If you start the backend manually on port 9090:

```bash
RUN21_REST_PORT=9090 ./gradlew :runtime:rest-api:run
```

Run pytest with the matching base URL:

```bash
RUN21_BASE_URL=http://localhost:9090 \
python -m pytest -c tests/system/pytest.ini tests/system/tests
```

If pytest starts the backend on port 9090:

```bash
RUN21_BASE_URL=http://localhost:9090 \
RUN21_START_SERVER=true \
RUN21_REST_PORT=9090 \
python -m pytest -c tests/system/pytest.ini tests/system/tests
```

`RUN21_REST_PORT` is consumed by the Gradle-launched REST runtime. `RUN21_BASE_URL` is consumed by
the Python test client and by the Rust CLI client wrapper.

### Custom server command

Override the command used by pytest-managed startup with `RUN21_SERVER_CMD`:

```bash
RUN21_START_SERVER=true \
RUN21_SERVER_CMD='./gradlew :runtime:rest-api:run' \
python -m pytest -c tests/system/pytest.ini tests/system/tests
```

The command is split with shell-like syntax by Python `shlex.split`. Keep it simple: executable plus
arguments. Environment variables such as `RUN21_REST_PORT` should be passed outside the command, as
shown in the custom port example.

## Configuration

Environment variables:

```text
RUN21_BASE_URL=http://localhost:8080
RUN21_CLI_BIN=./clients/run21ctl/target/debug/run21ctl
RUN21_START_SERVER=false
RUN21_SERVER_CMD=./gradlew :runtime:rest-api:run
```

- `RUN21_BASE_URL`: REST API base URL used by HTTP tests and passed to `run21ctl --base-url`.
- `RUN21_CLI_BIN`: path to the Rust CLI binary used by CLI smoke tests.
- `RUN21_START_SERVER`: when `true`, pytest starts `RUN21_SERVER_CMD` for the session.
- `RUN21_SERVER_CMD`: command used only when `RUN21_START_SERVER=true`.

The readiness wait has a 45 second timeout and polls `GET /ready` every 0.5 seconds.

## Selecting Tests

Run the full suite:

```bash
python -m pytest -c tests/system/pytest.ini tests/system/tests
```

Run only HTTP API tests and exclude CLI smoke tests:

```bash
python -m pytest -c tests/system/pytest.ini tests/system/tests -k 'not cli'
```

Run one test file:

```bash
python -m pytest -c tests/system/pytest.ini tests/system/tests/test_formula_execution.py
```

Show test names and xfail/skip reasons in the summary:

```bash
python -m pytest -c tests/system/pytest.ini tests/system/tests -ra -vv
```

## CLI Smoke Tests

The CLI tests use the same REST backend as the HTTP tests. Build the Rust client first:

```bash
cargo build --manifest-path clients/run21ctl/Cargo.toml
python -m pytest -c tests/system/pytest.ini tests/system/tests/test_cli_smoke.py
```

If `RUN21_CLI_BIN` does not point to an existing file, the CLI fixture skips the CLI tests. It does
not fail the HTTP API suite.

Use a custom CLI binary:

```bash
RUN21_CLI_BIN=/path/to/run21ctl \
python -m pytest -c tests/system/pytest.ini tests/system/tests/test_cli_smoke.py
```

## Test Case Matrix

| File | Case type | Contract checked | Expected result |
| --- | --- | --- | --- |
| `test_health.py` | Positive HTTP | `GET /health` exposes liveness. | HTTP 200 JSON with `status == "ok"`. |
| `test_ready.py` | Positive HTTP | `GET /ready` exposes runtime readiness and registered tools. | HTTP 200 JSON with `status == "ready"` and at least one tool. |
| `test_tools_api.py` | Positive HTTP | Tool discovery returns `formula.evaluate`. | Tool list contains `formula.evaluate`. |
| `test_tools_api.py` | Positive HTTP | Tool descriptor exposes stable metadata and schemas. | Descriptor has code `formula.evaluate`, status `done`, required input `expression`, and output property `value`. |
| `test_formula_execution.py` | Positive HTTP | Structured execution reaches `DispatcherChain -> ToolExecutor -> ToolImplementation -> ToolResult`. | Result has `status == "succeeded"`, `outputPayload.value == "21"`, formula tool audit metadata, and dispatcher trace. |
| `test_formula_execution.py` | Negative HTTP | Missing required formula input is returned as a structured tool error. | Result has `status` of `failed` or `rejected`, error code `INVALID_TOOL_REQUEST`, and an error message mentioning `expression`. |
| `test_cli_smoke.py` | Positive CLI | `run21ctl health` can call the backend. | JSON has `status == "ok"`. |
| `test_cli_smoke.py` | Positive CLI | `run21ctl tools` can list tools. | Tool list contains `formula.evaluate`. |
| `test_cli_smoke.py` | Positive CLI | `run21ctl execute` can execute the formula tool through REST. | Same successful formula result assertions as the HTTP execution test. |
| `test_prompt_resolution.py` | Future positive HTTP, marked `xfail` | Complete prompt input should resolve into a prepared `ToolRequest`. | Expected to fail until a raw/prompt dispatcher is registered in the REST runtime profile. |
| `test_prompt_resolution.py` | Future negative HTTP, marked `xfail` | Incomplete prompt input should be rejected with missing-field validation. | Expected to fail until a raw/prompt dispatcher is registered in the REST runtime profile. |

## Checking Results

Pytest prints a one-line status for each test when run with `-vv`:

```bash
python -m pytest -c tests/system/pytest.ini tests/system/tests -vv
```

Common outcomes:

- `PASSED`: the contract is currently satisfied.
- `FAILED`: the backend responded, but the response did not match the expected contract, or the
  backend did not become ready before the timeout.
- `SKIPPED`: the CLI binary was not available for CLI smoke tests.
- `XFAIL`: the prompt-resolution contract is documented but not active in the current REST runtime
  profile.
- `XPASS`: a prompt-resolution test unexpectedly passed; this usually means a raw/prompt dispatcher
  has been enabled and the xfail marker should be reviewed.

For API failures, pytest includes the response body in the assertion message for status-code checks.
For CLI failures, the helper reports the CLI stderr and stdout.

The most important success shape for execution tests is:

```json
{
  "status": "succeeded",
  "outputPayload": {
    "value": "21"
  },
  "auditMetadata": {
    "tool": {
      "code": "formula.evaluate"
    },
    "dispatcherTrace": [
      {
        "dispatcher": "pass-through"
      }
    ]
  }
}
```

This is a reduced example. `dispatcherTrace` only needs to be present and non-empty in the assertion.
The exact trace content can evolve as dispatcher-chain internals change.
