#!/usr/bin/env bash
set -euo pipefail

DISPATCHER_DEBUG="${RUN21_DISPATCHER_DEBUG:-false}"
if [[ "${1:-}" == "--dispatcher-debug" ]]; then
  DISPATCHER_DEBUG="true"
  shift
elif [[ "${1:-}" == "--no-dispatcher-debug" ]]; then
  DISPATCHER_DEBUG="false"
  shift
fi

if [[ $# -gt 0 ]]; then
  echo "Usage: $0 [--dispatcher-debug|--no-dispatcher-debug]" >&2
  exit 1
fi

export RUN21_REST_PROFILE="${RUN21_REST_PROFILE:-ollama}"
export RUN21_LLM_BASE_URL="${RUN21_LLM_BASE_URL:-http://localhost:11434}"
export RUN21_LLM_MODEL="${RUN21_LLM_MODEL:-qwen2.5:1.5b}"
export RUN21_LLM_TIMEOUT_MS="${RUN21_LLM_TIMEOUT_MS:-30000}"
export RUN21_LLM_TEMPERATURE="${RUN21_LLM_TEMPERATURE:-0.0}"
export RUN21_DISPATCHER_DEBUG="$DISPATCHER_DEBUG"

exec ./gradlew :runtime:rest-api:run
