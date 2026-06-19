package ai.run21.runtime.restapi

import ai.run21.core.model.ActorReference
import ai.run21.core.model.ActorSnapshot
import ai.run21.core.model.ActorType
import ai.run21.core.model.AuditMetadata
import ai.run21.core.model.CorrelationMetadata
import ai.run21.core.model.DispatcherChainResult
import ai.run21.core.model.DispatcherTraceStep
import ai.run21.core.model.ExecutionSource
import ai.run21.core.model.ExecutionSourceType
import ai.run21.core.model.ImplementationCode
import ai.run21.core.model.ImplementationSnapshot
import ai.run21.core.model.RawDispatchInput
import ai.run21.core.model.StructuredDispatchInput
import ai.run21.core.model.ToolCode
import ai.run21.core.model.ToolDescriptor
import ai.run21.core.model.ToolError
import ai.run21.core.model.ToolRequest
import ai.run21.core.model.ToolResult
import ai.run21.core.model.ToolSnapshot
import ai.run21.core.model.VersionMetadata
import ai.run21.core.model.VersionSnapshot

/**
 * Converts the single REST request shape into the dispatch input expected by core.
 *
 * This is the REST boundary where callers choose between structured execution and raw/prompt
 * dispatch. The core still sees only DispatchInput implementations.
 */
fun ExecutionRequestDto.toDispatchInput() = when (type.lowercase()) {
    "structured" -> StructuredDispatchInput(toToolRequest())
    "prompt", "raw" -> RawDispatchInput(
        payload = promptPayload(),
        actor = actor.toModel(),
        source = source.toModel(),
        correlation = correlation.toModel(),
        attributes = attributes,
    )
    else -> throw IllegalArgumentException("Execution request type must be structured, prompt, or raw.")
}

/**
 * Builds the canonical ToolRequest used for structured calls.
 *
 * This method intentionally performs only shape conversion and required-field checks. Tool-specific
 * validation remains in the dispatcher/executor path.
 */
fun ExecutionRequestDto.toToolRequest(): ToolRequest {
    val code = requireNotNull(toolCode) { "toolCode is required for structured requests." }
    return ToolRequest(
        targetTool = ToolCode(code),
        targetImplementation = implementationCode?.let(::ImplementationCode),
        inputPayload = input,
        actor = actor.toModel(),
        source = source.toModel(),
        correlation = correlation.toModel(),
        versionMetadata = version?.toModel(),
        metadata = metadata,
    )
}

/**
 * Converts a dispatcher-chain result into the resolve endpoint response.
 */
fun DispatcherChainResult.toDto(): ResolveResponseDto = ResolveResponseDto(
    status = status.wireValue,
    request = request?.toDto(),
    error = error?.toDto(),
    trace = trace.steps.map { it.toDto() },
)

/**
 * Converts a registered tool descriptor into the public discovery representation.
 */
fun ToolDescriptor.toDto(): ToolDescriptorDto = ToolDescriptorDto(
    code = code.value,
    name = name,
    description = description,
    status = status.wireValue,
    inputSchema = inputSchema.value,
    outputSchema = outputSchema.value,
)

/**
 * Converts a resolved core request into the public request representation.
 */
fun ToolRequest.toDto(): ToolRequestDto = ToolRequestDto(
    targetTool = targetTool.value,
    targetImplementation = targetImplementation?.value,
    inputPayload = inputPayload,
    actor = ActorDto(
        id = actor.id,
        type = actor.type.wireValue,
        displayName = actor.displayName,
    ),
    source = source.toDto(),
    correlation = correlation.toDto(),
    version = versionMetadata?.toDto(),
    metadata = metadata,
)

/**
 * Converts the canonical execution result into the REST response body.
 */
fun ToolResult.toDto(): ToolResultDto = ToolResultDto(
    status = status.wireValue,
    outputPayload = outputPayload,
    error = error?.toDto(),
    auditMetadata = auditMetadata.toDto(),
    metadata = metadata,
)

// Prompt text is exposed as the conventional `prompt` payload field while preserving any additional
// caller-provided raw payload fields.
private fun ExecutionRequestDto.promptPayload(): Map<String, Any?> =
    if (prompt == null) {
        payload
    } else {
        linkedMapOf<String, Any?>("prompt" to prompt).also { it.putAll(payload) }
    }

private fun ActorDto.toModel(): ActorReference = ActorReference(
    id = id,
    type = enumValue(type, ActorType.HUMAN),
    displayName = displayName,
)

private fun SourceDto.toModel(): ExecutionSource = ExecutionSource(
    type = enumValue(type, ExecutionSourceType.API),
    name = name,
    metadata = metadata,
)

// Missing correlation ids are generated here so HTTP callers can stay concise while the core still
// receives a complete CorrelationMetadata object.
private fun CorrelationDto?.toModel(): CorrelationMetadata = when {
    this == null -> CorrelationMetadata()
    executionId.isNullOrBlank() && correlationId.isNullOrBlank() -> CorrelationMetadata()
    executionId.isNullOrBlank() -> CorrelationMetadata(
        correlationId = requireNotNull(correlationId),
        causationId = causationId,
    )
    else -> CorrelationMetadata(
        executionId = executionId,
        correlationId = correlationId ?: executionId,
        causationId = causationId,
    )
}

private fun VersionDto.toModel(): VersionMetadata = VersionMetadata(
    definitionVersion = definitionVersion,
    implementationVersion = implementationVersion,
    metadata = metadata,
)

private fun ExecutionSource.toDto(): SourceDto = SourceDto(
    type = type.wireValue,
    name = name,
    metadata = metadata,
)

private fun CorrelationMetadata.toDto(): CorrelationDto = CorrelationDto(
    executionId = executionId,
    correlationId = correlationId,
    causationId = causationId,
)

private fun VersionMetadata.toDto(): VersionDto = VersionDto(
    definitionVersion = definitionVersion,
    implementationVersion = implementationVersion,
    metadata = metadata,
)

private fun VersionSnapshot.toDto(): VersionDto = VersionDto(
    definitionVersion = definitionVersion,
    implementationVersion = implementationVersion,
    metadata = metadata,
)

private fun ToolError.toDto(): ToolErrorDto = ToolErrorDto(
    code = code.wireValue,
    message = message,
    details = details,
)

private fun AuditMetadata.toDto(): AuditMetadataDto = AuditMetadataDto(
    actor = actor.toDto(),
    source = source.toDto(),
    dispatcherTrace = dispatcherTrace.steps.map { it.toDto() },
    tool = tool.toDto(),
    implementation = implementation?.toDto(),
    version = version?.toDto(),
    correlation = correlation.toDto(),
    startedAt = startedAt.toString(),
    completedAt = completedAt.toString(),
    metadata = metadata,
)

private fun ActorSnapshot.toDto(): ActorDto = ActorDto(
    id = id,
    type = type.wireValue,
    displayName = displayName,
)

private fun DispatcherTraceStep.toDto(): DispatcherTraceStepDto = DispatcherTraceStepDto(
    dispatcherCode = dispatcherCode.value,
    outcome = outcome.wireValue,
    confidence = confidence,
    error = error?.toDto(),
    inputSummary = inputSummary,
    outputSummary = outputSummary,
    correlationId = correlationId,
    metadata = metadata,
)

private fun ToolSnapshot.toDto(): ToolSnapshotDto = ToolSnapshotDto(
    code = code.value,
    name = name,
    status = status.wireValue,
    inputSchema = inputSchema.value,
    outputSchema = outputSchema.value,
)

private fun ImplementationSnapshot.toDto(): ImplementationSnapshotDto = ImplementationSnapshotDto(
    implementationCode = implementationCode.value,
    toolCode = toolCode.value,
    type = type.wireValue,
    version = version,
    status = status.wireValue,
    runtimeMetadata = runtimeMetadata,
)

// Enum fields are accepted case-insensitively to keep the HTTP API tolerant of common JSON styles.
private inline fun <reified T : Enum<T>> enumValue(value: String, default: T): T =
    enumValues<T>().firstOrNull { it.name.equals(value, ignoreCase = true) || it.name.lowercase() == value.lowercase() }
        ?: default
