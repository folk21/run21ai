package ai.run21.core.model

/**
 * Stable machine-readable error codes for core dispatch and execution failures.
 */
enum class ToolErrorCode {
    UNKNOWN_TOOL,
    UNSUPPORTED_TOOL,
    IMPLEMENTATION_NOT_REGISTERED,
    DISPATCHER_NOT_REGISTERED,
    INVALID_TOOL_REQUEST,
    DISPATCH_REJECTED,
    NO_DISPATCHER_ACCEPTED;

    val wireValue: String
        get() = name
}

/**
 * Structured error object returned in dispatch results and tool results.
 */
data class ToolError(
    val code: ToolErrorCode,
    val message: String,
    val details: Map<String, Any?> = emptyMap(),
) {
    init {
        require(message.isNotBlank()) { "Tool error message must not be blank." }
    }
}
