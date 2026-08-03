# run21ctl

`run21ctl` is the independent Rust CLI client for the Run21AI REST API.

Build and test it from the repository root:

```bash
cargo build --manifest-path clients/run21ctl/Cargo.toml
cargo test --manifest-path clients/run21ctl/Cargo.toml
```

The default backend URL is `http://127.0.0.1:8080`. Override it with `--base-url` or `RUN21_BASE_URL`.

Common commands:

```bash
cargo run --manifest-path clients/run21ctl/Cargo.toml -- health
cargo run --manifest-path clients/run21ctl/Cargo.toml -- ready
cargo run --manifest-path clients/run21ctl/Cargo.toml -- tools
cargo run --manifest-path clients/run21ctl/Cargo.toml -- tool formula.evaluate
```

Structured execution and prompt-resolution examples are owned by [Usage](../../docs/USAGE.md). Test integration is documented in [Testing](../../docs/TESTING.md).
