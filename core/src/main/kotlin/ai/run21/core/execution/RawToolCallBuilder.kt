package ai.run21.core.execution

import ai.run21.core.model.ActorReference
import ai.run21.core.model.ActorType
import ai.run21.core.model.CorrelationMetadata
import ai.run21.core.model.ExecutionSource
import ai.run21.core.model.ExecutionSourceType
import ai.run21.core.model.RawDispatchInput

/**
 * Fluent builder for raw dispatcher-chain calls.
 *
 * This builder is used when the caller does not already have a canonical tool request. The caller
 * submits prompt-like or partially structured data, and the configured dispatcher chain is
 * responsible for converting that data into a ToolRequest.
 */
class RawToolCallBuilder {
    private var actor: ActorReference? = null
    private var source: ExecutionSource? = null
    private var correlation: CorrelationMetadata = CorrelationMetadata()
    private val payload = linkedMapOf<String, Any?>()
    private val attributes = linkedMapOf<String, Any?>()

    /**
     * Sets the actor that initiated the raw dispatch request.
     */
    fun actor(id: String, type: ActorType, displayName: String? = null) {
        actor = ActorReference(
            id = id,
            type = type,
            displayName = displayName,
        )
    }

    /**
     * Sets the actor that initiated the raw dispatch request.
     */
    fun actor(reference: ActorReference) {
        actor = reference
    }

    /**
     * Sets the source that submitted the raw dispatch request.
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
     * Sets the source that submitted the raw dispatch request.
     */
    fun source(value: ExecutionSource) {
        source = value
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
     * Adds or replaces one raw payload field.
     */
    fun payload(name: String, value: Any?) {
        require(name.isNotBlank()) { "Payload name must not be blank." }
        payload[name] = value
    }

    /**
     * Adds or replaces several raw payload fields.
     */
    fun payload(values: Map<String, Any?>) {
        values.forEach { (name, value) -> payload(name, value) }
    }

    /**
     * Convenience shortcut for prompt-based dispatchers.
     */
    fun prompt(value: String) {
        payload("prompt", value)
    }

    /**
     * Adds or replaces one dispatcher attribute.
     */
    fun attribute(name: String, value: Any?) {
        require(name.isNotBlank()) { "Attribute name must not be blank." }
        attributes[name] = value
    }

    /**
     * Adds or replaces several dispatcher attributes.
     */
    fun attributes(values: Map<String, Any?>) {
        values.forEach { (name, value) -> attribute(name, value) }
    }

    /**
     * Builds raw input for the configured dispatcher chain.
     */
    fun build(): RawDispatchInput {
        val requestActor = requireNotNull(actor) { "Actor is required." }
        val requestSource = requireNotNull(source) { "Execution source is required." }

        return RawDispatchInput(
            payload = payload.toMap(),
            actor = requestActor,
            source = requestSource,
            correlation = correlation,
            attributes = attributes.toMap(),
        )
    }
}
