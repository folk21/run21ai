package ai.run21.core.execution

import ai.run21.core.model.DispatchInput
import ai.run21.core.model.ToolResult

/**
 * Main execution service contract.
 *
 * Every execution starts from DispatchInput, passes through a DispatcherChain, and is
 * then handled by the resolved ToolExecutor. Direct structured calls are represented as
 * structured dispatch input handled by a pass-through dispatcher.
 */
interface ToolExecutionService {
    /**
     * Executes input through the configured dispatcher chain and tool execution pipeline.
     */
    suspend fun execute(input: DispatchInput): ToolResult
}
