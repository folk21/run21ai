from __future__ import annotations

import subprocess
import time
from collections.abc import Iterator
from contextlib import contextmanager

from run21ai_system_tests.api_client import Run21ApiClient
from run21ai_system_tests.config import SystemTestConfig


def wait_until_ready(client: Run21ApiClient, timeout_seconds: float = 45.0) -> None:
    deadline = time.monotonic() + timeout_seconds
    last_error: Exception | None = None
    while time.monotonic() < deadline:
        try:
            response = client.ready()
            if response.status_code == 200:
                return
        except Exception as error:  # noqa: BLE001 - readiness loop preserves last failure.
            last_error = error
        time.sleep(0.5)
    raise AssertionError(f"Run21AI backend did not become ready: {last_error}")


@contextmanager
def optional_server(config: SystemTestConfig) -> Iterator[None]:
    if not config.start_server:
        yield
        return

    process = subprocess.Popen(config.server_cmd)
    try:
        yield
    finally:
        process.terminate()
        try:
            process.wait(timeout=10)
        except subprocess.TimeoutExpired:
            process.kill()
