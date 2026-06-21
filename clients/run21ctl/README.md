# run21ctl

`run21ctl` is a Rust CLI client for the Run21AI REST API.

It is intentionally independent from the Gradle build. Use Cargo commands directly from the
repository root.

## Prerequisite: Cargo

Cargo is installed with the Rust toolchain. On macOS or Linux, install it with `rustup`:

```bash
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
```

Then open a new shell or reload your shell profile and verify:

```bash
cargo --version
```

## Build and Test

```bash
cargo test --manifest-path clients/run21ctl/Cargo.toml
```

## Backend URL

The CLI defaults to:

```text
http://127.0.0.1:8080
```

Override it with either a global flag or an environment variable:

```bash
cargo run --manifest-path clients/run21ctl/Cargo.toml -- \
  --base-url http://127.0.0.1:9090 health

RUN21_BASE_URL=http://127.0.0.1:9090 \
  cargo run --manifest-path clients/run21ctl/Cargo.toml -- health
```

## Commands

```bash
cargo run --manifest-path clients/run21ctl/Cargo.toml -- health
cargo run --manifest-path clients/run21ctl/Cargo.toml -- ready
cargo run --manifest-path clients/run21ctl/Cargo.toml -- tools
cargo run --manifest-path clients/run21ctl/Cargo.toml -- tool formula.evaluate
```

Execute a structured formula request:

```bash
cargo run --manifest-path clients/run21ctl/Cargo.toml -- execute \
  --tool-code formula.evaluate \
  --implementation-code formula-evalex-v1 \
  --input-json '{"expression":"a + b","variables":{"a":10,"b":11}}' \
  --actor-id manual-user \
  --actor-name "Manual User" \
  --source-name run21ctl \
  --correlation-id run21ctl-manual-test
```

Resolve the same request without executing it:

```bash
cargo run --manifest-path clients/run21ctl/Cargo.toml -- resolve \
  --tool-code formula.evaluate \
  --implementation-code formula-evalex-v1 \
  --input-json '{"expression":"a + b","variables":{"a":10,"b":11}}' \
  --actor-id manual-user
```

Prompt-like requests are supported by the CLI request shape, but the current formula-only backend
profile rejects them until a raw-input dispatcher is registered:

```bash
cargo run --manifest-path clients/run21ctl/Cargo.toml -- resolve \
  --prompt "calculate 10 + 11" \
  --actor-id manual-user
```
