package ai.run21.core.execution

import ai.run21.core.model.AuditMetadata
import ai.run21.core.model.ExecutionStatus
import ai.run21.core.model.ToolError
import ai.run21.core.model.ToolResult

/**
 * Builds successful and failed ToolResult values with consistent status, payload, error, and audit
 * metadata handling.
 */
object ToolResultFactory {
    /**
     * Creates a successful result with output payload and audit metadata.
     */
    fun succeeded(
        outputPayload: Map<String, Any?>,
        auditMetadata: AuditMetadata,
        metadata: Map<String, Any?> = emptyMap(),
    ): ToolResult =
        ToolResult(
            status = ExecutionStatus.SUCCEEDED,
            outputPayload = outputPayload,
            auditMetadata = auditMetadata,
            metadata = metadata,
        )

    /**
     * Creates a failed result for execution errors after a tool path was selected.
     */
    fun failed(
        error: ToolError,
        auditMetadata: AuditMetadata,
        metadata: Map<String, Any?> = emptyMap(),
    ): ToolResult =
        ToolResult(
            status = ExecutionStatus.FAILED,
            error = error,
            auditMetadata = auditMetadata,
            metadata = metadata,
        )

    /**
     * Creates a rejected result for requests stopped before normal tool execution.
     */
    fun rejected(
        error: ToolError,
        auditMetadata: AuditMetadata,
        metadata: Map<String, Any?> = emptyMap(),
    ): ToolResult =
        ToolResult(
            status = ExecutionStatus.REJECTED,
            error = error,
            auditMetadata = auditMetadata,
            metadata = metadata,
        )
}
