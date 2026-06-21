from run21ai_system_tests.api_client import Run21ApiClient
from run21ai_system_tests.assertions import assert_formula_result, assert_structured_error, json_body
from run21ai_system_tests.payloads import load_fixture


def test_structured_formula_request_succeeds(api_client: Run21ApiClient) -> None:
    response = api_client.execute(load_fixture("formula/evaluate_valid.json"))
    body = json_body(response)

    assert_formula_result(body)


def test_formula_request_with_missing_required_parameter_returns_structured_error(
    api_client: Run21ApiClient,
) -> None:
    response = api_client.execute(load_fixture("formula/evaluate_missing_expression.json"))
    body = json_body(response)

    assert_structured_error(body, "INVALID_TOOL_REQUEST")
    assert "expression" in body["error"]["message"].lower()
