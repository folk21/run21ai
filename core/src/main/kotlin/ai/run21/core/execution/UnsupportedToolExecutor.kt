package ai.run21.core.execution

import ai.run21.core.model.ToolCode
import ai.run21.core.model.ToolDescriptor
import ai.run21.core.model.ToolError
import ai.run21.core.model.ToolErrorCode
import ai.run21.core.model.ToolRequest
import ai.run21.core.model.ToolResult

/**
 * Safe fallback executor for missing tool execution support. It preserves audit metadata while
 * returning a structured UNSUPPORTED_TOOL error.
 */
class UnsupportedToolExecutor(
    override val toolCode: ToolCode,
    private val descriptor: ToolDescriptor? = null,
    private val auditMetadataFactory: AuditMetadataFactory = AuditMetadataFactory(),
) : ToolExecutor {
    override suspend fun execute(request: ToolRequest): ToolResult {
        val auditMetadata = auditMetadataFactory.create(
            request = request,
            toolDescriptor = descriptor,
            implementationDescriptor = null,
        )
        return ToolResultFactory.failed(
            error = ToolError(
                code = ToolErrorCode.UNSUPPORTED_TOOL,
                message = "No tool executor is registered for the target tool.",
                details = mapOf("toolCode" to toolCode.value),
            ),
            auditMetadata = auditMetadata,
        )
    }
}
