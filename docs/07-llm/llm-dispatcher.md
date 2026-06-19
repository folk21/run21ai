# LLM Dispatcher

## Purpose

An LLM dispatcher converts unstructured input into a structured `ToolRequest`.

It is one dispatcher type inside the dispatcher chain.

## Responsibilities

An LLM dispatcher may:

- classify input intent;
- select a target tool;
- extract structured arguments;
- identify missing fields;
- produce confidence metadata;
- reject unsafe or incomplete input.

An LLM dispatcher must not perform tool work.

## Output Contract

The output must be valid structured data that can be validated before tool execution.

## Failure Handling

Invalid structured output must be rejected before tool execution.

The dispatcher chain may retry, pass to another dispatcher, or return a structured dispatch error.
