package ai.run21.dispatchers.llm

import ai.run21.core.model.ActorReference
import ai.run21.core.model.CorrelationMetadata
import ai.run21.core.model.ExecutionSource
import ai.run21.core.model.ToolErrorCode
import ai.run21.core.model.VersionMetadata

/**
 * Normalized prompt sent from the dispatcher to an LLM-style client.
 */
data class LlmDispatchPrompt(
    val payload: Map<String, Any?>,
    val actor: ActorReference,
    val source: ExecutionSource,
    val correlation: CorrelationMetadata,
    val attributes: Map<String, Any?> = emptyMap(),
)

/**
 * Structured response returned by an LLM-style client.
 */
sealed interface LlmDispatchResponse {
    val confidence: Double?
    val inputSummary: String?
    val outputSummary: String?
    val metadata: Map<String, Any?>
}

/**
 * Response that contains a complete candidate ToolRequest.
 */
data class LlmDispatchAccepted(
    val targetTool: String,
    val targetImplementation: String? = null,
    val inputPayload: Map<String, Any?>,
    val requestMetadata: Map<String, Any?> = emptyMap(),
    val versionMetadata: VersionMetadata? = null,
    override val confidence: Double? = null,
    override val inputSummary: String? = null,
    override val outputSummary: String? = null,
    override val metadata: Map<String, Any?> = emptyMap(),
) : LlmDispatchResponse

/**
 * Response that declines execution before a ToolRequest is produced.
 */
data class LlmDispatchRejected(
    val message: String,
    val errorCode: ToolErrorCode = ToolErrorCode.DISPATCH_REJECTED,
    val details: Map<String, Any?> = emptyMap(),
    override val confidence: Double? = null,
    override val inputSummary: String? = null,
    override val outputSummary: String? = null,
    override val metadata: Map<String, Any?> = emptyMap(),
) : LlmDispatchResponse {
    init {
        require(message.isNotBlank()) { "LLM dispatch rejection message must not be blank." }
    }
}
