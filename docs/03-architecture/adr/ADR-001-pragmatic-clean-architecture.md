# ADR-001: Use pragmatic clean architecture

## Status

Accepted

## Context

The core must define execution contracts without depending on runtime frameworks.

## Decision

Use a pragmatic clean architecture style:

- core contracts stay framework-independent;
- runtime modules depend on core;
- concrete frameworks, persistence, clients, and integrations stay outside core.

## Consequences

- Core is easy to test.
- Runtime choices can change without changing core contracts.
- Excessive ports/adapters ceremony is avoided.
