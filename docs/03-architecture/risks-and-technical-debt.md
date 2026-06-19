# Risks and Technical Debt

## Terminology Drift

Risk: multiple terms for the same concept.

Mitigation: use the glossary terms consistently.

## Overengineering

Risk: adding complex extension mechanisms before the execution model is proven.

Mitigation: define contracts first and implement small vertical slices.

## LLM Overreach

Risk: LLM dispatchers perform work instead of preparing tool requests.

Mitigation: tool work is performed only by registered tool executors and implementations.

## Weak Audit Metadata

Risk: results cannot explain how a request was routed or executed.

Mitigation: every result includes dispatcher trace and implementation snapshot metadata.
