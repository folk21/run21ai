from run21ai_system_tests.assertions import assert_formula_result
from run21ai_system_tests.cli_client import Run21CliClient


def test_run21ctl_health_works(cli_client: Run21CliClient) -> None:
    body = cli_client.run_json("health")

    assert body["status"] == "ok"


def test_run21ctl_tools_list_works(cli_client: Run21CliClient) -> None:
    tools = cli_client.run_json("tools")

    assert "formula.evaluate" in {tool["code"] for tool in tools}


def test_run21ctl_execute_formula_works(cli_client: Run21CliClient) -> None:
    body = cli_client.run_json(
        "execute",
        "--tool-code",
        "formula.evaluate",
        "--implementation-code",
        "formula-evalex-v1",
        "--input-json",
        '{"expression":"a + b","variables":{"a":10,"b":11}}',
        "--actor-id",
        "system-test-cli-user",
        "--correlation-id",
        "system-test-cli-formula",
    )

    assert_formula_result(body)
