from run21ai_system_tests.api_client import Run21ApiClient
from run21ai_system_tests.assertions import json_body


def test_health_returns_200(api_client: Run21ApiClient) -> None:
    body = json_body(api_client.health())

    assert body["status"] == "ok"
