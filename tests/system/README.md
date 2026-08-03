# Run21AI System Tests

This package contains pytest black-box tests for the REST API and `run21ctl`.

Install from the repository root:

```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install -e tests/system
```

Run against an existing backend:

```bash
python -m pytest -c tests/system/pytest.ini tests/system/tests
```

Let pytest start and stop the backend:

```bash
RUN21_START_SERVER=true \
python -m pytest -c tests/system/pytest.ini tests/system/tests
```

The complete configuration table, custom-port examples, CLI smoke-test setup, and test-layer ownership are maintained in [Testing](../../docs/TESTING.md).
