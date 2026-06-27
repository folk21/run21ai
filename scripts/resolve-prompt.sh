#!/usr/bin/env bash
set -euo pipefail

# Resolves a natural-language prompt through the Run21AI REST dispatcher chain.
#
# This script does not execute a tool. It calls `run21ctl resolve`, which sends a prompt-like
# request to `POST /v1/tool-requests/resolve`. When the REST backend is started with the Ollama
# dispatcher profile, the dispatcher chain asks the configured Ollama model to convert the prompt
# into a structured `ToolRequest`, for example a `formula.evaluate` request with an input payload.
#
# Start the backend first, for example:
#
#   ./scripts/start-rest-ollama.sh --dispatcher-debug
#
# Then run:
#
#   ./scripts/resolve-prompt.sh \
#     "Prepare a formula.evaluate request for expression a + b with variables a = 10 and b = 11."
#
# Add `--dispatcher-debug` to ask the backend dispatcher to print the raw Ollama request and
# response for this request. This is request-scoped; `start-rest-ollama.sh --dispatcher-debug`
# enables the same logging for every request handled by that backend process.
#
# The optional second positional argument overrides the correlation id used in audit/debug output.
# The CLI backend URL can be overridden with RUN21_BASE_URL or `run21ctl --base-url`, but this
# helper intentionally keeps the default CLI URL behavior.
DISPATCHER_DEBUG="false"
if [[ "${1:-}" == "--dispatcher-debug" ]]; then
  DISPATCHER_DEBUG="true"
  shift
elif [[ "${1:-}" == "--no-dispatcher-debug" ]]; then
  DISPATCHER_DEBUG="false"
  shift
fi

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 [--dispatcher-debug|--no-dispatcher-debug] \"<prompt>\" [correlation-id]" >&2
  exit 1
fi

PROMPT="$1"
CORRELATION_ID="${2:-cli-llm-resolve-test}"
ATTRIBUTES_JSON="{\"dispatcherDebug\":$DISPATCHER_DEBUG}"

exec cargo run --manifest-path clients/run21ctl/Cargo.toml -- resolve \
  --prompt "$PROMPT" \
  --attributes-json "$ATTRIBUTES_JSON" \
  --actor-id manual-user \
  --correlation-id "$CORRELATION_ID"
