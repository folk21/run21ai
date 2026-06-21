from __future__ import annotations

import pytest

from run21ai_system_tests.api_client import Run21ApiClient
from run21ai_system_tests.cli_client import Run21CliClient
from run21ai_system_tests.config import SystemTestConfig, load_config
from run21ai_system_tests.wait import optional_server, wait_until_ready


@pytest.fixture(scope="session")
def system_config() -> SystemTestConfig:
    return load_config()


@pytest.fixture(scope="session")
def api_client(system_config: SystemTestConfig) -> Run21ApiClient:
    client = Run21ApiClient(system_config.base_url)
    with optional_server(system_config):
        wait_until_ready(client)
        yield client


@pytest.fixture(scope="session")
def cli_client(system_config: SystemTestConfig, api_client: Run21ApiClient) -> Run21CliClient:
    client = Run21CliClient(system_config.cli_bin, system_config.base_url)
    if not client.available():
        pytest.skip(f"Rust CLI binary is not available: {system_config.cli_bin}")
    return client
