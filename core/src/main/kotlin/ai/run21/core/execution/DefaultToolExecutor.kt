package ai.run21.core.execution

import ai.run21.core.dispatch.DefaultToolRequestValidator
import ai.run21.core.dispatch.ToolRequestValidationResult
import ai.run21.core.dispatch.ToolRequestValidator
import ai.run21.core.model.ToolCode
import ai.run21.core.model.ToolDescriptor
import ai.run21.core.model.ToolError
import ai.run21.core.model.ToolErrorCode
import ai.run21.core.model.ToolRequest
import ai.run21.core.model.ToolResult
import ai.run21.core.registry.ToolImplementationRegistry

/**
 * Default executor for a single tool descriptor. It resolves the requested implementation and
 * converts implementation output into the canonical ToolResult.
 */
class DefaultToolExecutor(
    private val descriptor: ToolDescriptor,
    private val implementationRegistry: ToolImplementationRegistry,
    private val validator: ToolRequestValidator = DefaultToolRequestValidator,
    private val auditMetadataFactory: AuditMetadataFactory = AuditMetadataFactory(),
) : ToolExecutor {
    override val toolCode: ToolCode = descriptor.code

    /**
     * Executes a validated request for this executor's tool.
     *
     * The executor resolves the selected implementation and converts the implementation output
     * into a canonical ToolResult with audit metadata.
     */
    override suspend fun execute(request: ToolRequest): ToolResult {
        // Guard against accidental registry misrouting. A tool executor must never run a request
        // intended for a different tool.
        if (request.targetTool != toolCode) {
            val auditMetadata = auditMetadataFactory.create(
                request = request,
                toolDescriptor = descriptor,
                implementationDescriptor = null,
            )
            return ToolResultFactory.failed(
                error = ToolError(
                    code = ToolErrorCode.INVALID_TOOL_REQUEST,
                    message = "Tool executor received a request for a different target tool.",
                    details = mapOf(
                        "expectedTool" to toolCode.value,
                        "actualTool" to request.targetTool.value,
                    ),
                ),
                auditMetadata = auditMetadata,
            )
        }

        // Apply core-level validation before resolving an implementation.
        when (val validation = validator.validate(request)) {
            ToolRequestValidationResult.Valid -> Unit
            is ToolRequestValidationResult.Invalid -> {
                val auditMetadata = auditMetadataFactory.create(
                    request = request,
                    toolDescriptor = descriptor,
                    implementationDescriptor = null,
                )
                return ToolResultFactory.failed(validation.error, auditMetadata)
            }
        }

        // If the request names a concrete implementation, resolve that exact implementation.
        // Otherwise the registry decides which default implementation should be used.
        val implementation = implementationRegistry.find(toolCode, request.targetImplementation)
        if (implementation == null) {
            val auditMetadata = auditMetadataFactory.create(
                request = request,
                toolDescriptor = descriptor,
                implementationDescriptor = null,
            )
            return ToolResultFactory.failed(
                error = ToolError(
                    code = ToolErrorCode.IMPLEMENTATION_NOT_REGISTERED,
                    message = "No matching tool implementation is registered for the target tool.",
                    details = mapOf(
                        "toolCode" to toolCode.value,
                        "implementationCode" to request.targetImplementation?.value,
                    ),
                ),
                auditMetadata = auditMetadata,
            )
        }

        val startedAt = java.time.Instant.now()

        // Real work happens in the implementation. The executor only owns routing, validation,
        // result wrapping, and audit metadata creation.
        val output = implementation.execute(request)
        val auditMetadata = auditMetadataFactory.create(
            request = request,
            toolDescriptor = descriptor,
            implementationDescriptor = implementation.descriptor,
            startedAt = startedAt,
        )
        return ToolResultFactory.succeeded(
            outputPayload = output.outputPayload,
            auditMetadata = auditMetadata,
            metadata = output.metadata,
        )
    }
}
