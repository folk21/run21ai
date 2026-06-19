# Dispatcher Chain

## Purpose

A dispatcher chain converts input into a structured `ToolRequest`.

The chain supports direct structured calls, rule-based dispatching, LLM dispatching, validation, enrichment, and custom routing through one execution model.

## Contract

A dispatcher receives dispatch input and returns one outcome:

- `accepted`: produced a `ToolRequest`;
- `enriched`: added context and passes control forward;
- `rejected`: produced a structured failure;
- `passed`: did not handle the input.

The dispatcher chain evaluates dispatchers in order until it receives an accepted or rejected outcome, or until no dispatcher can handle the input.

## Dispatcher Types

### Pass-through Dispatcher

Accepts already structured input and produces a `ToolRequest` without semantic analysis.

### Rule-based Dispatcher

Uses deterministic rules to validate, enrich, or route input.

### LLM Dispatcher

Uses a local or external LLM to convert unstructured input into a structured tool request.

### Validation Dispatcher

Validates a tool request before execution.

### Custom Dispatcher

Uses project-specific logic to normalize, enrich, or route input.

## Example Chains

### Structured Direct Chain

```text
PassThroughDispatcher
  -> ValidationDispatcher
  -> ToolRequest
```

### LLM Chain

```text
RuleBasedDispatcher
  -> LlmDispatcher
  -> ValidationDispatcher
  -> ToolRequest
```

### Custom Chain

```text
CustomDispatcher
  -> ValidationDispatcher
  -> ToolRequest
```

## Trace Requirements

Each dispatcher step must record trace metadata:

- dispatcher code;
- outcome;
- confidence when applicable;
- structured errors when applicable;
- input/output summary;
- correlation identifier.

Dispatcher trace becomes part of audit metadata.
