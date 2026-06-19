# Testing Strategy

## Core Contract Tests

Validate abstract contracts without runtime frameworks.

Required areas:

- dispatcher chain outcomes;
- pass-through dispatcher behavior;
- tool request validation;
- tool executor registry behavior;
- unsupported defaults;
- tool result audit metadata.

## Tool Implementation Tests

Validate concrete tools when runtime implementations are added.

Examples:

- formula tool evaluates valid input;
- formula tool rejects missing variables;
- custom tool returns structured output;
- spreadsheet tool returns expected mapped output.

## API Tests

Validate HTTP behavior when runtime API is added.

## LLM Dispatcher Tests

Use fake dispatchers for normal tests.

Use real LLM evaluation separately.

Check:

- valid structured output;
- invalid output rejection;
- missing field detection;
- no tool work inside dispatcher.
