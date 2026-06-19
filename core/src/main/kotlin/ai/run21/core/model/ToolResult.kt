package ai.run21.core.model

/**
 * Canonical response returned from tool execution. It carries status, output, structured error
 * data, audit metadata, and caller-facing metadata.
 */
data class ToolResult(
    val status: ExecutionStatus,
    val outputPayload: Map<String, Any?> = emptyMap(),
    val error: ToolError? = null,
    val auditMetadata: AuditMetadata,
    val metadata: Map<String, Any?> = emptyMap(),
) {
    init {
        require(status == ExecutionStatus.SUCCEEDED || error != null) {
            "Non-succeeded ToolResult must contain a structured error."
        }
        require(status != ExecutionStatus.SUCCEEDED || error == null) {
            "Succeeded ToolResult must not contain an error."
        }
    }
}
