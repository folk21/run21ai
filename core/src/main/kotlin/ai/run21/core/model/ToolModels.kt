package ai.run21.core.model

/**
 * Public description of a tool capability known by the core.
 *
 * A descriptor is metadata only. It does not execute work and does not expose any
 * runtime framework dependency.
 */
data class ToolDescriptor(
    val code: ToolCode,
    val name: String,
    val description: String,
    val inputSchema: ContractSchema = ContractSchema(),
    val outputSchema: ContractSchema = ContractSchema(),
    val status: CapabilityStatus = CapabilityStatus.PLANNED,
) {
    init {
        require(name.isNotBlank()) { "Tool name must not be blank." }
        require(description.isNotBlank()) { "Tool description must not be blank." }
    }
}

/**
 * Implementation category used for routing, audit, and documentation.
 *
 * The enum is intentionally broad. Runtime modules may map these categories to JVM
 * classes, formula engines, spreadsheets, HTTP adapters, local services, or custom code.
 */
enum class ToolImplementationType {
    JVM_CLASS,
    FORMULA,
    SPREADSHEET,
    HTTP_ADAPTER,
    LOCAL_SERVICE,
    CUSTOM;

    val wireValue: String
        get() = name.lowercase()
}

/**
 * Public description of a concrete implementation registered for a tool.
 */
data class ToolImplementationDescriptor(
    val implementationCode: ImplementationCode,
    val toolCode: ToolCode,
    val type: ToolImplementationType,
    val version: String? = null,
    val status: CapabilityStatus = CapabilityStatus.PLANNED,
    val runtimeMetadata: Map<String, Any?> = emptyMap(),
)

/**
 * Optional version metadata attached to a tool request and copied into audit data.
 *
 * The core does not define version storage rules. It only carries version snapshots
 * across the execution boundary.
 */
data class VersionMetadata(
    val definitionVersion: String? = null,
    val implementationVersion: String? = null,
    val metadata: Map<String, Any?> = emptyMap(),
)

/**
 * Structured request produced by a dispatcher chain and accepted by tool execution.
 *
 * Direct calls also use this model: a pass-through dispatcher accepts a structured
 * request without changing it. The optional target implementation narrows execution to
 * a specific registered implementation when the caller or dispatcher has that knowledge.
 */
data class ToolRequest(
    val targetTool: ToolCode,
    val inputPayload: Map<String, Any?>,
    val actor: ActorReference,
    val source: ExecutionSource,
    val targetImplementation: ImplementationCode? = null,
    val dispatcherTrace: DispatcherTrace = DispatcherTrace(),
    val versionMetadata: VersionMetadata? = null,
    val correlation: CorrelationMetadata = CorrelationMetadata(),
    val metadata: Map<String, Any?> = emptyMap(),
)

/**
 * Raw output returned by a concrete tool implementation before it is wrapped into a
 * ToolResult with status, errors, and audit metadata.
 */
data class ToolExecutionOutput(
    val outputPayload: Map<String, Any?>,
    val metadata: Map<String, Any?> = emptyMap(),
)
