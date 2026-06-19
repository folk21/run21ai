package ai.run21.runtime.restapi

/**
 * HTTP representation of the actor that submitted a request.
 *
 * This mirrors the core actor reference but keeps the REST wire format independent from core
 * inline/value types and enum serialization rules.
 */
data class ActorDto(
    val id: String = "",
    val type: String = "human",
    val displayName: String? = null,
)

/**
 * HTTP representation of the entry point that submitted a request.
 */
data class SourceDto(
    val type: String = "api",
    val name: String? = null,
    val metadata: Map<String, Any?> = emptyMap(),
)

/**
 * Optional correlation identifiers supplied by API callers.
 *
 * Missing values are completed by the mapping layer so the core always receives valid correlation
 * metadata.
 */
data class CorrelationDto(
    val executionId: String? = null,
    val correlationId: String? = null,
    val causationId: String? = null,
)

/**
 * Optional tool definition and implementation version metadata carried into audit output.
 */
data class VersionDto(
    val definitionVersion: String? = null,
    val implementationVersion: String? = null,
    val metadata: Map<String, Any?> = emptyMap(),
)

/**
 * Request accepted by both execution and resolve endpoints.
 *
 * `type = structured` is converted into a StructuredDispatchInput. `type = prompt` or `raw` is
 * converted into RawDispatchInput and therefore requires a raw-input dispatcher in the runtime
 * profile.
 */
data class ExecutionRequestDto(
    val type: String = "structured",
    val toolCode: String? = null,
    val implementationCode: String? = null,
    val input: Map<String, Any?> = emptyMap(),
    val prompt: String? = null,
    val payload: Map<String, Any?> = emptyMap(),
    val attributes: Map<String, Any?> = emptyMap(),
    val actor: ActorDto = ActorDto(),
    val source: SourceDto = SourceDto(),
    val correlation: CorrelationDto? = null,
    val version: VersionDto? = null,
    val metadata: Map<String, Any?> = emptyMap(),
)

/**
 * Minimal liveness response.
 */
data class HealthResponseDto(
    val status: String,
)

/**
 * Readiness response with the number of registered tool descriptors visible to the API runtime.
 */
data class ReadyResponseDto(
    val status: String,
    val tools: Int,
)

/**
 * Response from request resolution.
 *
 * This endpoint reports the dispatcher-chain result only. It never contains tool execution output.
 */
data class ResolveResponseDto(
    val status: String,
    val request: ToolRequestDto? = null,
    val error: ToolErrorDto? = null,
    val trace: List<DispatcherTraceStepDto> = emptyList(),
)

/**
 * Public tool descriptor returned by discovery endpoints.
 */
data class ToolDescriptorDto(
    val code: String,
    val name: String,
    val description: String,
    val status: String,
    val inputSchema: Map<String, Any?>,
    val outputSchema: Map<String, Any?>,
)

/**
 * Resolved canonical tool request returned by the resolve endpoint.
 */
data class ToolRequestDto(
    val targetTool: String,
    val targetImplementation: String? = null,
    val inputPayload: Map<String, Any?>,
    val actor: ActorDto,
    val source: SourceDto,
    val correlation: CorrelationDto,
    val version: VersionDto? = null,
    val metadata: Map<String, Any?> = emptyMap(),
)

/**
 * HTTP representation of the canonical ToolResult.
 */
data class ToolResultDto(
    val status: String,
    val outputPayload: Map<String, Any?> = emptyMap(),
    val error: ToolErrorDto? = null,
    val auditMetadata: AuditMetadataDto,
    val metadata: Map<String, Any?> = emptyMap(),
)

/**
 * Structured error body used by dispatch, execution, and HTTP request validation failures.
 */
data class ToolErrorDto(
    val code: String,
    val message: String,
    val details: Map<String, Any?> = emptyMap(),
)

/**
 * Audit metadata returned with every execution result.
 */
data class AuditMetadataDto(
    val actor: ActorDto,
    val source: SourceDto,
    val dispatcherTrace: List<DispatcherTraceStepDto>,
    val tool: ToolSnapshotDto,
    val implementation: ImplementationSnapshotDto? = null,
    val version: VersionDto? = null,
    val correlation: CorrelationDto,
    val startedAt: String,
    val completedAt: String,
    val metadata: Map<String, Any?> = emptyMap(),
)

/**
 * One dispatcher-chain trace step exposed for debugging and auditability.
 */
data class DispatcherTraceStepDto(
    val dispatcherCode: String,
    val outcome: String,
    val confidence: Double? = null,
    val error: ToolErrorDto? = null,
    val inputSummary: String? = null,
    val outputSummary: String? = null,
    val correlationId: String? = null,
    val metadata: Map<String, Any?> = emptyMap(),
)

/**
 * Immutable snapshot of the tool descriptor used during execution.
 */
data class ToolSnapshotDto(
    val code: String,
    val name: String,
    val status: String,
    val inputSchema: Map<String, Any?>,
    val outputSchema: Map<String, Any?>,
)

/**
 * Immutable snapshot of the concrete implementation selected for execution.
 */
data class ImplementationSnapshotDto(
    val implementationCode: String,
    val toolCode: String,
    val type: String,
    val version: String? = null,
    val status: String,
    val runtimeMetadata: Map<String, Any?> = emptyMap(),
)
