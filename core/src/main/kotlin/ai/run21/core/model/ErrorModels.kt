package ai.run21.core.model

/**
 * Stable machine-readable error codes for core dispatch and execution failures.
 */
enum class ToolErrorCode {
    /**
     * The dispatcher chain produced a request for a tool code that is not registered in the
     * active ToolRegistry.
     */
    UNKNOWN_TOOL,

    /**
     * The target tool is known, but the runtime has no executor capable of executing it. The core
     * usually returns this through UnsupportedToolExecutor.
     */
    UNSUPPORTED_TOOL,

    /**
     * The target tool has an executor, but no matching ToolImplementation is registered for the
     * requested implementation code or for the tool default.
     */
    IMPLEMENTATION_NOT_REGISTERED,

    /**
     * No dispatcher chain or dispatcher is available to normalize input into a ToolRequest.
     */
    DISPATCHER_NOT_REGISTERED,

    /**
     * A ToolRequest violates core or tool-specific validation rules before implementation
     * execution starts.
     */
    INVALID_TOOL_REQUEST,

    /**
     * A dispatcher made a terminal rejection decision, usually because input was unsafe,
     * ambiguous, incomplete, or outside the supported contract.
     */
    DISPATCH_REJECTED,

    /**
     * The dispatcher chain completed without any dispatcher accepting or rejecting the input.
     */
    NO_DISPATCHER_ACCEPTED,

    /**
     * A dispatcher or dispatcher-chain implementation failed unexpectedly while preparing a
     * ToolRequest.
     */
    DISPATCHER_FAILED,

    /**
     * A ToolExecutor or ToolImplementation failed unexpectedly after a tool path was selected.
     */
    TOOL_EXECUTION_FAILED,

    /**
     * Dispatch or tool execution exceeded its configured time budget.
     */
    TIMEOUT;

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
