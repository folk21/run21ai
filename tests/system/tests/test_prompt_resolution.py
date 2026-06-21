import pytest

from run21ai_system_tests.api_client import Run21ApiClient
from run21ai_system_tests.assertions import json_body
from run21ai_system_tests.payloads import complete_prompt_payload, incomplete_prompt_payload


@pytest.mark.xfail(reason="No raw/prompt dispatcher is registered in the current REST runtime profile.")
def test_complete_prompt_returns_prepared_tool_request(api_client: Run21ApiClient) -> None:
    body = json_body(api_client.resolve(complete_prompt_payload()))

    assert body["status"] == "accepted"
    assert body["request"]["targetTool"] == "formula.evaluate"
    assert body["request"]["inputPayload"]["expression"]


@pytest.mark.xfail(reason="No raw/prompt dispatcher is registered in the current REST runtime profile.")
def test_incomplete_prompt_returns_missing_fields_error(api_client: Run21ApiClient) -> None:
    body = json_body(api_client.resolve(incomplete_prompt_payload()))

    assert body["status"] == "rejected"
    assert body["error"]["code"] == "INVALID_TOOL_REQUEST"
    assert "missing" in body["error"]["message"].lower()
