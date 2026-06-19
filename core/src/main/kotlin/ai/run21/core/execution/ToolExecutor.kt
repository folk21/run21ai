package ai.run21.core.execution

import ai.run21.core.model.ToolCode
import ai.run21.core.model.ToolRequest
import ai.run21.core.model.ToolResult

/**
 * Tool-level execution boundary.
 *
 * A tool executor validates a ToolRequest, resolves a concrete ToolImplementation, and
 * wraps implementation output into a ToolResult with status and audit metadata.
 */
interface ToolExecutor {
    /**
     * Tool code handled by this executor.
     */
    val toolCode: ToolCode

    /**
     * Executes a canonical tool request and returns a canonical result.
     */
    suspend fun execute(request: ToolRequest): ToolResult
}
