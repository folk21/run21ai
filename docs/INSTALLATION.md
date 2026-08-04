# Installation

Run commands from the repository root, the directory containing `gradlew` and `settings.gradle.kts`.

## Required

- JDK 21
- A shell supported by the Gradle wrapper (`gradlew` on macOS/Linux or `gradlew.bat` on Windows)

Verify the environment:

```bash
java -version
./gradlew --version
```

No separate Gradle installation is required.

## Optional Components

| Component | Required for |
|---|---|
| Rust toolchain and Cargo | Building and testing `clients/run21ctl` |
| Python 3.11+ and pytest | Running black-box system tests |
| Ollama and an installed model | Running the Ollama-backed REST profile |

Verify optional tools when needed:

```bash
cargo --version
python --version
ollama --version
```

## Build the Project

```bash
./gradlew build
```

## Run the Default REST Profile

The default profile registers the formula tool and accepts structured requests through `PassThroughDispatcher`.

```bash
./gradlew :runtime:rest-api:run
```

The server listens on `0.0.0.0:8080` by default.

Override the host or port:

```bash
RUN21_REST_HOST=127.0.0.1 \
RUN21_REST_PORT=9090 \
./gradlew :runtime:rest-api:run
```

## Run the Ollama Profile

Install a model in Ollama, then start the REST runtime with the LLM dispatcher profile:

```bash
RUN21_REST_PROFILE=ollama \
RUN21_LLM_MODEL=<installed-model> \
./gradlew :runtime:rest-api:run
```

Supported environment variables:

| Variable | Default | Purpose |
|---|---|---|
| `RUN21_REST_PROFILE` | formula-only profile | Use `ollama` or `llm-ollama` to enable raw prompt dispatch |
| `RUN21_REST_HOST` | `0.0.0.0` | REST bind host |
| `RUN21_REST_PORT` | `8080` | REST bind port |
| `RUN21_LLM_MODEL` | none | Required Ollama model name |
| `RUN21_LLM_BASE_URL` | `http://localhost:11434` | Ollama server URL |
| `RUN21_LLM_TIMEOUT_MS` | `30000` | Ollama request timeout |
| `RUN21_LLM_TEMPERATURE` | `0.0` | Ollama generation temperature |
| `RUN21_DISPATCHER_DEBUG` | `false` | Prints redacted dispatcher transport metadata without prompt or response bodies |

The current Ollama adapter supports formula request preparation only. The `/ready` endpoint calls Ollama's `/api/tags` endpoint and returns `503` until the server is reachable and `RUN21_LLM_MODEL` is installed. See [LLM Dispatch](architecture/LLM_DISPATCH.md) for the boundary and limitations.

## Rust CLI

Build the client independently from Gradle:

```bash
cargo build --manifest-path clients/run21ctl/Cargo.toml
```

## Python System Tests

Create an isolated environment and install the test package:

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -e tests/system
```

On Windows PowerShell, activate the environment with:

```powershell
.venv\Scripts\Activate.ps1
```

See [Testing](TESTING.md) for test commands and runtime configuration.
