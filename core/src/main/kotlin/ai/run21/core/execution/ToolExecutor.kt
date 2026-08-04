package ai.run21.core.execution

import ai.run21.core.model.ImplementationCode
import ai.run21.core.model.ToolCode
import ai.run21.core.model.ToolImplementationDescriptor
import ai.run21.core.model.ToolRequest
import ai.run21.core.model.ToolResult

/**
 * Registration metadata exposed by an executor for runtime assembly validation.
 */
data class ToolExecutorRegistration(
    val toolCode: ToolCode,
    val implementations: List<ToolImplementationDescriptor>,
    val defaultImplementation: ImplementationCode?,
)

/**
 * Tool-level execution boundary.
 *
 * A tool executor validates a ToolRequest, resolves a concrete ToolImplementation, and wraps
 * implementation output into a ToolResult with status and audit metadata.
 */
interface ToolExecutor {
    /** Tool code handled by this executor. */
    val toolCode: ToolCode

    /**
     * Registration metadata used by the standard runtime assembly.
     *
     * Custom executors that participate in [ai.run21.core.CoreToolRuntime.inMemoryAssembly] must
     * expose their registered implementations and explicit default. Low-level execution services
     * may still use executors without this metadata.
     */
    val registration: ToolExecutorRegistration?
        get() = null

    /** Executes a canonical tool request and returns a canonical result. */
    suspend fun execute(request: ToolRequest): ToolResult
}
