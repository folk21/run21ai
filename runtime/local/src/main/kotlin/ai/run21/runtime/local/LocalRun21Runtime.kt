package ai.run21.runtime.local

import ai.run21.core.CoreToolRuntime
import ai.run21.core.execution.RawToolCallBuilder
import ai.run21.core.execution.ToolCallBuilder
import ai.run21.core.model.ToolResult

/**
 * Local in-memory runtime facade over a fully wired CoreToolRuntime.
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
