from __future__ import annotations

import json
import subprocess
from pathlib import Path
from typing import Any


class Run21CliClient:
    def __init__(self, cli_bin: Path, base_url: str) -> None:
        self.cli_bin = cli_bin
        self.base_url = base_url

    def available(self) -> bool:
        return self.cli_bin.exists() and self.cli_bin.is_file()

    def run(self, *args: str) -> subprocess.CompletedProcess[str]:
        return subprocess.run(
            [str(self.cli_bin), "--base-url", self.base_url, *args],
            check=False,
            text=True,
            capture_output=True,
            timeout=20,
        )

    def run_json(self, *args: str) -> Any:
        result = self.run(*args)
        if result.returncode != 0:
            raise AssertionError(f"run21ctl failed: {result.stderr}\nstdout: {result.stdout}")
        return json.loads(result.stdout)
