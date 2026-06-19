# Core Module Contract

## Purpose

The first implementation target is the abstract core module.

The module defines models, interfaces, contracts, status types, errors, audit metadata, registries, dispatcher-chain contracts, tool-executor contracts, and unsupported defaults.

It does not contain concrete runtime implementations.

## Core Questions

The core module must answer:

- What is input?
- What is a dispatcher?
- What is a dispatcher chain?
- What is a tool request?
- What is a tool?
- What is a tool executor?
- What is a tool implementation?
- What is a tool result?
- How is unsupported behavior represented?
- How is audit metadata attached to every result?

## Model Groups

### Actor Models

- actor reference;
- actor type;
- execution source.

### Dispatch Models

- dispatch input;
- dispatch context;
- dispatch outcome;
- dispatcher trace;
- dispatcher chain.

### Tool Models

- tool code;
- tool descriptor;
- tool request;
- tool result;
- tool error;
- tool executor;
- tool implementation descriptor;
- tool registry.

### Audit Models

- actor snapshot;
- dispatcher trace snapshot;
- tool snapshot;
- implementation snapshot;
- version snapshot when applicable;
- correlation identifiers.

## Services and Registries

The core defines contracts for:

- dispatcher chain execution;
- tool request validation;
- tool executor resolution;
- tool implementation resolution;
- tool execution;
- unsupported default behavior.

## Unsupported Defaults

The core provides safe defaults for:

- unknown tool;
- unsupported tool;
- missing implementation;
- missing dispatcher;
- invalid tool request.

Defaults must fail explicitly and return structured errors.

## Contract Tests

Contract tests must verify:

- framework-independent core dependencies;
- pass-through dispatching;
- dispatcher-chain outcomes;
- tool executor registry behavior;
- unsupported defaults;
- tool result audit metadata.
