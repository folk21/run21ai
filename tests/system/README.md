# Run21AI System Tests

This pytest module verifies Run21AI through external contracts:

```text
pytest
  -> HTTP API / Rust CLI
  -> runtime:rest-api
  -> DispatcherChain
  -> ToolExecutor
  -> ToolImplementation
  -> ToolResult
```

The tests are independent from Gradle and are intended to run against a real REST API process.

## Configuration

Environment variables:

```text
RUN21_BASE_URL=http://localhost:8080
RUN21_CLI_BIN=./clients/run21ctl/target/debug/run21ctl
RUN21_START_SERVER=false
RUN21_SERVER_CMD=./gradlew :runtime:rest-api:run
```

`RUN21_START_SERVER=false` means the backend must already be running.

## Install

From the repository root, create a Python virtual environment and install the system-test package:

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -e tests/system
```

## Run

From the repository root:

```bash
pytest tests/system
```

Start the backend automatically:

```bash
RUN21_START_SERVER=true pytest tests/system
```

Run only HTTP API tests:

```bash
pytest tests/system/tests -k 'not cli'
```

Run CLI smoke tests after building the Rust client:

```bash
cargo build --manifest-path clients/run21ctl/Cargo.toml
pytest tests/system/tests/test_cli_smoke.py
```

The prompt-resolution tests document the future contract for a raw/prompt dispatcher. They are
marked `xfail` until such a dispatcher is registered in the REST runtime profile.
