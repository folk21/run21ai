from __future__ import annotations

import os
import shlex
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class SystemTestConfig:
    base_url: str
    cli_bin: Path
    start_server: bool
    server_cmd: list[str]


def load_config() -> SystemTestConfig:
    return SystemTestConfig(
        base_url=os.getenv("RUN21_BASE_URL", "http://localhost:8080").rstrip("/"),
        cli_bin=Path(os.getenv("RUN21_CLI_BIN", "./clients/run21ctl/target/debug/run21ctl")),
        start_server=os.getenv("RUN21_START_SERVER", "false").lower() == "true",
        server_cmd=shlex.split(os.getenv("RUN21_SERVER_CMD", "./gradlew :runtime:rest-api:run")),
    )
