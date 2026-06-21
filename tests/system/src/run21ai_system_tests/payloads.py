from __future__ import annotations

import json
from pathlib import Path
from typing import Any


FIXTURES_DIR = Path(__file__).resolve().parents[2] / "fixtures"


def load_fixture(relative_path: str) -> dict[str, Any]:
    path = FIXTURES_DIR / relative_path
    return json.loads(path.read_text(encoding="utf-8"))


def complete_prompt_payload() -> dict[str, Any]:
    return {
        "type": "prompt",
        "prompt": "Evaluate formula a + b with a = 10 and b = 11 using formula.evaluate.",
        "actor": {"id": "system-test-user", "type": "human"},
        "source": {"type": "api", "name": "pytest"},
        "correlation": {"correlationId": "system-test-prompt-complete"},
    }


def incomplete_prompt_payload() -> dict[str, Any]:
    return {
        "type": "prompt",
        "prompt": "Evaluate the formula.",
        "actor": {"id": "system-test-user", "type": "human"},
        "source": {"type": "api", "name": "pytest"},
        "correlation": {"correlationId": "system-test-prompt-incomplete"},
    }
