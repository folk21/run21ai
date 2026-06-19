# Global Code Generation Rules

## Language

All code comments, documentation, configuration comments, and generated text must be in English.

## Scope

Generate small slices.

Do not generate runtime framework code inside the core module.

## Terminology

Use the project glossary terms:

- Dispatcher
- DispatcherChain
- ToolRequest
- ToolExecutor
- ToolImplementation
- ToolResult
- AuditMetadata

## Core Rules

The core module must define contracts and safe unsupported defaults.

It must not depend on HTTP frameworks, persistence frameworks, LLM clients, expression engines, or CLI libraries.

## Test Rules

Every generated slice must include relevant tests.
