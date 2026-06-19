# REQ-AUDIT-001: Versioning and audit metadata

Status: planned

## Goal

The system must make tool execution reproducible and auditable.

## Functional Requirements

- Tool execution must include actor metadata.
- Tool execution must include execution source metadata.
- Tool execution must include dispatcher trace metadata.
- Tool execution must include tool descriptor snapshot metadata.
- Tool execution must include implementation descriptor snapshot metadata.
- Tool execution may include version metadata when the target tool uses versioned definitions.
- Tool result must include structured output or structured error.

## Acceptance Criteria

- Given a tool execution succeeds
  When the result is returned
  Then audit metadata identifies actor, tool, implementation, source, and dispatcher trace.

- Given a tool execution fails
  When the result is returned
  Then audit metadata is still present.

- Given a tool uses versioned definitions
  When execution finishes
  Then the result includes the exact version metadata used.

## Test Cases

- `TEST-AUDIT-001`: Successful execution returns audit metadata.
- `TEST-AUDIT-002`: Failed execution returns audit metadata.
- `TEST-AUDIT-003`: Versioned tool execution returns version metadata.
