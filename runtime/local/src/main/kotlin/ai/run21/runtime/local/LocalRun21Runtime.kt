package ai.run21.runtime.local

import ai.run21.core.execution.RawToolCallBuilder
import ai.run21.core.execution.ToolCallBuilder
import ai.run21.core.execution.ToolCalls
import ai.run21.core.execution.ToolExecutionService
import ai.run21.core.model.ToolResult

/**
 * Local in-memory runtime facade over a fully wired ToolExecutionService.
 */
class LocalRun21Runtime(
    val service: ToolExecutionService,
) {
    /**
     * Executes a structured tool call through the configured dispatcher chain.
     */
    suspend fun execute(block: ToolCallBuilder.() -> Unit): ToolResult =
        ToolCalls.execute(service, block)

    /**
     * Dispatches raw input through the configured dispatcher chain.
     */
    suspend fun dispatch(block: RawToolCallBuilder.() -> Unit): ToolResult =
        ToolCalls.dispatch(service, block)
}
