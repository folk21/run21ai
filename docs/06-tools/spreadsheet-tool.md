# Spreadsheet Tool

## Purpose

A spreadsheet tool executes spreadsheet-style templates or engines using structured input mappings.

## Contract

A spreadsheet tool may define:

- template reference;
- input mapping;
- output mapping;
- recompute rules;
- generated artifact metadata.

## Default Behavior

If no spreadsheet implementation is registered, execution returns `UNSUPPORTED_TOOL`.
