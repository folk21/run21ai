from run21ai_system_tests.api_client import Run21ApiClient
from run21ai_system_tests.assertions import json_body


def test_ready_returns_200_and_runtime_checks(api_client: Run21ApiClient) -> None:
    body = json_body(api_client.ready())

    assert body["status"] == "ready"
    assert body["tools"] >= 1
    assert body["executors"] >= 1
    assert body["implementations"] >= 1
    registry = next(check for check in body["checks"] if check["name"] == "registry-assembly")
    assert registry["status"] == "ready"
    assert registry["details"]["defaultImplementations"]
