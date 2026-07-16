# run21ctl

`clients/run21ctl` contains `run21ctl`, a Rust command-line client for the Run21AI REST API.
It is useful for manual checks, scripted calls, and API contract validation without writing `curl`
payloads by hand.

The client calls the REST API only. Execution still follows the dispatcher-chain model:

```text
run21ctl -> REST API -> DispatcherChain -> ToolRequest -> ToolExecutor -> ToolImplementation -> ToolResult
```

## Build and Test

The client is intentionally independent from the Gradle build. Build or test it with Cargo from the
project root:

```bash
cargo test --manifest-path clients/run21ctl/Cargo.toml
```

Run commands with:

```bash
cargo run --manifest-path clients/run21ctl/Cargo.toml -- <command>
```

## Backend URL

The default backend URL is `http://127.0.0.1:8080`. Override it with the global `--base-url` flag or
the `RUN21_BASE_URL` environment variable:

```bash
cargo run --manifest-path clients/run21ctl/Cargo.toml -- \
  --base-url http://127.0.0.1:9090 health

RUN21_BASE_URL=http://127.0.0.1:9090 \
  cargo run --manifest-path clients/run21ctl/Cargo.toml -- health
```

## Commands

```text
run21ctl health
  call GET /health

run21ctl ready
  call GET /ready

run21ctl tools
  call GET /v1/tools

run21ctl tool <code>
  call GET /v1/tools/{code}

run21ctl resolve ...
  call POST /v1/tool-requests/resolve

run21ctl execute ...
  call POST /v1/executions
```

All command responses are printed as pretty JSON. Non-`200 OK` responses are reported as command
errors with the HTTP status and response body.

## Structured Execution

Structured execution names the target tool and sends a JSON input object. The request still passes
through the dispatcher chain before the `ToolExecutor` invokes the selected `ToolImplementation`.

```bash
cargo run --manifest-path clients/run21ctl/Cargo.toml -- execute \
  --tool-code formula.evaluate \
  --implementation-code formula-evalex-v1 \
  --input-json '{"expression":"a + b","variables":{"a":10,"b":11}}' \
  --actor-id api-doc-user \
  --actor-name "API Documentation User" \
  --source-name run21ctl \
  --correlation-id run21ctl-structured-formula
```

Use `resolve` with the same request flags to inspect the canonical `ToolRequest` without executing
tool work:

```bash
cargo run --manifest-path clients/run21ctl/Cargo.toml -- resolve \
  --tool-code formula.evaluate \
  --implementation-code formula-evalex-v1 \
  --input-json '{"expression":"a + b","variables":{"a":10,"b":11}}' \
  --actor-id api-doc-user \
  --correlation-id run21ctl-resolve-formula
```

## Prompt Requests

Prompt-like requests use `--prompt`, which makes the client send `type: "prompt"` to the REST API.
They require a raw-input dispatcher in the configured dispatcher chain. The default formula-only
runtime rejects them with a structured dispatch error.

```bash
cargo run --manifest-path clients/run21ctl/Cargo.toml -- resolve \
  --prompt "Evaluate formula a + b with a = 10 and b = 11 using formula.evaluate." \
  --actor-id api-doc-user \
  --correlation-id run21ctl-prompt-formula
```

For prompt-like requests, optional JSON objects can be provided with `--payload-json` and
`--attributes-json` when a dispatcher needs additional raw payload or classification attributes.

