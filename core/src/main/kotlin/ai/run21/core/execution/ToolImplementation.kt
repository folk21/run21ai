package ai.run21.core.execution

import ai.run21.core.model.ToolExecutionOutput
import ai.run21.core.model.ToolImplementationDescriptor
import ai.run21.core.model.ToolRequest

/**
 * Concrete executable implementation of a tool.
 *
 * Implementations perform real work. The core only defines this contract; runtime
 * modules provide implementations for formula engines, services, local code, external
 * adapters, spreadsheets, and other execution mechanisms.
 */
interface ToolImplementation {
    /**
     * Metadata that identifies this implementation and the tool it belongs to.
     */
    val descriptor: ToolImplementationDescriptor

    /**
     * Performs implementation-specific work and returns raw execution output.
     */
    suspend fun execute(request: ToolRequest): ToolExecutionOutput
}
