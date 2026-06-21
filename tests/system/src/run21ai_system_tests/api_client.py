from __future__ import annotations

from typing import Any

import requests


class Run21ApiClient:
    def __init__(self, base_url: str) -> None:
        self.base_url = base_url.rstrip("/")

    def get(self, path: str) -> requests.Response:
        return requests.get(self._url(path), timeout=10)

    def post(self, path: str, payload: dict[str, Any]) -> requests.Response:
        return requests.post(self._url(path), json=payload, timeout=10)

    def health(self) -> requests.Response:
        return self.get("/health")

    def ready(self) -> requests.Response:
        return self.get("/ready")

    def tools(self) -> requests.Response:
        return self.get("/v1/tools")

    def tool(self, code: str) -> requests.Response:
        return self.get(f"/v1/tools/{code}")

    def execute(self, payload: dict[str, Any]) -> requests.Response:
        return self.post("/v1/executions", payload)

    def resolve(self, payload: dict[str, Any]) -> requests.Response:
        return self.post("/v1/tool-requests/resolve", payload)

    def _url(self, path: str) -> str:
        return f"{self.base_url}/{path.lstrip('/')}"
