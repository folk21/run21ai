# Capability Status

| Capability | Status | Contract | Default Behavior | Concrete Runtime |
|---|---|---|---|---|
| Abstract core | planned | yes | N/A | core module |
| Dispatcher chain | planned | yes | Reject invalid input | pass-through chain |
| Pass-through dispatcher | planned | yes | N/A | core default |
| Tool registry | planned | yes | Reject unknown tool | in-memory registry |
| Tool executor registry | planned | yes | Reject unsupported tool | in-memory registry |
| Formula-oriented tool | planned | yes | Unsupported failure | expression-based runtime |
| Custom tool | planned | yes | Unsupported failure | custom runtime |
| Spreadsheet-style tool | planned | yes | Unsupported failure | spreadsheet runtime |
| LLM dispatcher | planned | yes | Not registered | Ollama-compatible runtime |
| CLI client | planned | HTTP contract | N/A | Kotlin CLI |
