from run21ai_system_tests.api_client import Run21ApiClient
from run21ai_system_tests.assertions import json_body


def test_tools_contains_formula_evaluate(api_client: Run21ApiClient) -> None:
    tools = json_body(api_client.tools())

    assert "formula.evaluate" in {tool["code"] for tool in tools}


def test_tool_descriptor_returns_schema(api_client: Run21ApiClient) -> None:
    descriptor = json_body(api_client.tool("formula.evaluate"))

    assert descriptor["code"] == "formula.evaluate"
    assert descriptor["status"] == "done"
    assert descriptor["inputSchema"]["required"] == ["expression"]
    assert "value" in descriptor["outputSchema"]["properties"]
