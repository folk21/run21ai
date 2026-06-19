package ai.run21.core.execution

import ai.run21.core.model.ActorReference
import ai.run21.core.model.ActorType
import ai.run21.core.model.CorrelationMetadata
import ai.run21.core.model.ExecutionSource
import ai.run21.core.model.ExecutionSourceType
import ai.run21.core.model.ImplementationCode
import ai.run21.core.model.ToolCode
import ai.run21.core.model.ToolRequest
import ai.run21.core.model.VersionMetadata

/**
 * Fluent builder for direct structured tool calls.
 *
 * The builder is a convenience API over the canonical [ToolRequest] model. It does not introduce
 * a second execution path: the built request is still sent through a dispatcher chain as
 * [ai.run21.core.model.StructuredDispatchInput].
 */
class ToolCallBuilder {
    private var targetTool: ToolCode? = null
    private var targetImplementation: ImplementationCode? = null
    private var actor: ActorReference? = null
    private var source: ExecutionSource? = null
    private var versionMetadata: VersionMetadata? = null
    private var correlation: CorrelationMetadata = CorrelationMetadata()
    private val inputPayload = linkedMapOf<String, Any?>()
    private val metadata = linkedMapOf<String, Any?>()

    /**
     * Sets the target tool code.
     */
    fun tool(code: String) {
        targetTool = ToolCode(code)
    }

    /**
     * Sets the target tool code from an already constructed identifier.
     */
    fun tool(code: ToolCode) {
        targetTool = code
    }

    /**
     * Narrows the request to a specific implementation of the target tool.
     */
    fun implementation(code: String) {
        targetImplementation = ImplementationCode(code)
    }

    /**
     * Narrows the request to a specific implementation of the target tool.
     */
    fun implementation(code: ImplementationCode) {
        targetImplementation = code
    }

    /**
     * Sets the actor that initiated execution.
     */
    fun actor(id: String, type: ActorType, displayName: String? = null) {
        actor = ActorReference(
            id = id,
            type = type,
            displayName = displayName,
        )
    }

    /**
     * Sets the actor that initiated execution.
     */
    fun actor(reference: ActorReference) {
        actor = reference
    }

    /**
     * Sets the source that submitted the request.
     */
    fun source(
        type: ExecutionSourceType,
        name: String? = null,
        metadata: Map<String, Any?> = emptyMap(),
    ) {
        source = ExecutionSource(
            type = type,
            name = name,
            metadata = metadata,
        )
    }

    /**
     * Sets the source that submitted the request.
     */
    fun source(value: ExecutionSource) {
        source = value
    }

    /**
     * Adds version metadata that will be copied into audit metadata.
     */
    fun version(
        definitionVersion: String? = null,
        implementationVersion: String? = null,
        metadata: Map<String, Any?> = emptyMap(),
    ) {
        versionMetadata = VersionMetadata(
            definitionVersion = definitionVersion,
            implementationVersion = implementationVersion,
            metadata = metadata,
        )
    }

    /**
     * Sets all correlation identifiers explicitly.
     */
    fun correlation(
        executionId: String,
        correlationId: String = executionId,
        causationId: String? = null,
    ) {
        correlation = CorrelationMetadata(
            executionId = executionId,
            correlationId = correlationId,
            causationId = causationId,
        )
    }

    /**
     * Sets only the correlation id while keeping the generated execution id.
     */
    fun correlationId(value: String) {
        correlation = correlation.copy(correlationId = value)
    }

    /**
     * Adds or replaces one input payload field.
     */
    fun input(name: String, value: Any?) {
        require(name.isNotBlank()) { "Input name must not be blank." }
        inputPayload[name] = value
    }

    /**
     * Adds or replaces several input payload fields.
     */
    fun inputs(values: Map<String, Any?>) {
        values.forEach { (name, value) -> input(name, value) }
    }

    /**
     * Adds or replaces one request metadata field.
     */
    fun metadata(name: String, value: Any?) {
        require(name.isNotBlank()) { "Metadata name must not be blank." }
        metadata[name] = value
    }

    /**
     * Adds or replaces several request metadata fields.
     */
    fun metadata(values: Map<String, Any?>) {
        values.forEach { (name, value) -> metadata(name, value) }
    }

    /**
     * Builds the canonical [ToolRequest].
     */
    fun build(): ToolRequest {
        val tool = requireNotNull(targetTool) { "Target tool code is required." }
        val requestActor = requireNotNull(actor) { "Actor is required." }
        val requestSource = requireNotNull(source) { "Execution source is required." }

        return ToolRequest(
            targetTool = tool,
            targetImplementation = targetImplementation,
            inputPayload = inputPayload.toMap(),
            actor = requestActor,
            source = requestSource,
            versionMetadata = versionMetadata,
            correlation = correlation,
            metadata = metadata.toMap(),
        )
    }
}
