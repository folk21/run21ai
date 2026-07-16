package ai.run21.core

import ai.run21.core.execution.RawToolCallBuilder
import ai.run21.core.execution.ToolCallBuilder
import ai.run21.core.execution.ToolCalls
import ai.run21.core.execution.ToolExecutionService
import ai.run21.core.model.DispatchInput
import ai.run21.core.model.ToolResult

/**
 * Root-level facade for calling a configured core tool execution pipeline.
 *
 * `CoreToolRuntime` is intentionally small: it gives callers a visible entry point directly under
 * the `ai.run21.core` package while preserving the core execution model. The facade does not select
 * tools, resolve implementations, run dispatchers itself, or create a second execution path.
 *
 * All calls are delegated to the supplied [ToolExecutionService]. Structured and raw builder-based
 * calls additionally go through [ToolCalls], so they keep the same behavior as direct use of the
 * recommended high-level API:
 *
 * - structured calls are wrapped as `StructuredDispatchInput`;
 * - raw calls are built as `RawDispatchInput`;
 * - every call still enters the configured dispatcher chain before tool execution;
 * - the resolved `ToolExecutor` and `ToolImplementation` remain responsible for real work.
 *
 * Runtime modules can wrap this facade with application-specific concerns such as HTTP, CLI, or
 * local in-memory profiles. Those concerns should stay outside `core`.
 */
class CoreToolRuntime(
    /**
     * The configured execution boundary used by this facade.
     *
     * Keeping the service visible lets advanced callers inspect or pass the underlying core service
     * to lower-level APIs without duplicating runtime wiring.
     */
    val executionService: ToolExecutionService,
) {
    /**
     * Executes already-built dispatch input through the configured dispatcher chain.
     *
     * Use this overload when the caller already has a [DispatchInput], for example a
     * `StructuredDispatchInput` produced elsewhere or a raw input object built without the DSL.
     */
    suspend fun execute(input: DispatchInput): ToolResult =
        executionService.execute(input)

    /**
     * Builds and executes a structured tool request.
     *
     * This is the facade equivalent of [ToolCalls.execute]. The builder creates a `ToolRequest`,
     * `ToolCalls` wraps it as structured dispatch input, and [executionService] runs the normal
     * dispatcher-chain-to-executor pipeline.
     */
    suspend fun execute(block: ToolCallBuilder.() -> Unit): ToolResult =
        ToolCalls.execute(executionService, block)

    /**
     * Builds and dispatches raw or prompt-like input.
     *
     * This is the facade equivalent of [ToolCalls.dispatch]. The configured dispatcher chain decides
     * whether the input can be normalized into a `ToolRequest` or should be rejected with a
     * structured error.
     */
    suspend fun dispatch(block: RawToolCallBuilder.() -> Unit): ToolResult =
        ToolCalls.dispatch(executionService, block)
}
