from __future__ import annotations

from typing import Any

import requests


def assert_status(response: requests.Response, expected: int = 200) -> None:
    assert response.status_code == expected, response.text


def json_body(response: requests.Response) -> Any:
    assert_status(response)
    return response.json()


def assert_structured_error(body: dict[str, Any], expected_code: str) -> None:
    assert body["status"] in {"failed", "rejected"}
    assert body["error"]["code"] == expected_code
    assert body["error"]["message"]


def assert_formula_result(body: dict[str, Any], expected_value: str = "21") -> None:
    assert body["status"] == "succeeded"
    assert str(body["outputPayload"]["value"]) == expected_value
    assert body["auditMetadata"]["tool"]["code"] == "formula.evaluate"
    assert body["auditMetadata"]["dispatcherTrace"]
