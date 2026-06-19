# Custom Tool

## Purpose

A custom tool executes project-specific logic behind the generic tool contract.

## Contract

A custom tool must expose:

- tool descriptor;
- input schema;
- output schema;
- implementation descriptor;
- stable error codes.

## Default Behavior

If no custom implementation is registered, execution returns `UNSUPPORTED_TOOL`.
