package ai.run21.runtime.local

import ai.run21.core.CoreToolRuntime
import ai.run21.core.execution.RawToolCallBuilder
import ai.run21.core.execution.ToolCallBuilder
import ai.run21.core.model.ToolResult

/**
 * Local in-process runtime facade over a fully wired [CoreToolRuntime].
 *
 * "Local" means this runtime is intended for callers that want to execute tools directly inside
 * the current JVM process, without going through the REST API, a CLI process, remote transport, or
 * external runtime container. It is useful for examples, tests, simple embedded applications, and
 * development profiles where the application can assemble the dispatcher chain, tool descriptors,
 * executors, and implementations in memory.
 *
 * This class does not define a separate execution model. It delegates to [CoreToolRuntime], which
 * delegates to the configured core execution service. Calls still follow the required core path:
 * input enters a dispatcher chain, becomes a `ToolRequest`, is handled by a `ToolExecutor`, invokes
 * a `ToolImplementation`, and returns a canonical `ToolResult`.
 *
 * Runtime-specific wiring belongs outside this class. Factory objects such as
 * [LocalRun21RuntimeFactory] decide which tools and dispatcher chain profile are registered; this
 * facade only exposes convenient structured and raw call methods over that prepared runtime.
 */
class LocalRun21Runtime(
    val core: CoreToolRuntime,
) {
    /**
     * Executes a structured tool call through the configured dispatcher chain.
     */
    suspend fun execute(block: ToolCallBuilder.() -> Unit): ToolResult =
        core.execute(block)

    /**
     * Dispatches raw input through the configured dispatcher chain.
     */
    suspend fun dispatch(block: RawToolCallBuilder.() -> Unit): ToolResult =
        core.dispatch(block)
}
