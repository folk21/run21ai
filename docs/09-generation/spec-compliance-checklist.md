# Spec Compliance Checklist

- [ ] Core has no runtime framework dependencies.
- [ ] All execution passes through a dispatcher chain.
- [ ] Direct structured calls use a pass-through dispatcher.
- [ ] Dispatcher output is validated before tool execution.
- [ ] Tool execution uses `ToolRequest -> ToolExecutor -> ToolImplementation -> ToolResult`.
- [ ] Unsupported tools fail with structured errors.
- [ ] Tool results contain audit metadata.
- [ ] Terminology matches the glossary.
- [ ] Tests map to requirements.
