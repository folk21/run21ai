package ai.run21.dispatchers.llm.ollama

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Request body for Ollama's non-streaming `/api/chat` endpoint.
 */
data class OllamaChatRequest(
    val model: String,
    val stream: Boolean = false,
    val format: Map<String, Any?>,
    val options: Map<String, Any?> = emptyMap(),
    val messages: List<OllamaChatMessage>,
)

/**
 * One chat message sent to or returned from Ollama.
 */
data class OllamaChatMessage(
    val role: String,
    val content: String,
)

/**
 * Response envelope returned by Ollama's chat endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class OllamaChatResponse(
    val message: OllamaChatMessage? = null,
    val done: Boolean = false,
) {
    val content: String = message?.content.orEmpty()
}

/**
 * JSON object that the model is instructed to place inside the generated text.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class OllamaDispatchJson(
    val decision: String? = null,
    val targetTool: String? = null,
    val targetImplementation: String? = null,
    val inputPayload: Map<String, Any?> = emptyMap(),
    val confidence: Double? = null,
    val message: String? = null,
    val details: Map<String, Any?> = emptyMap(),
    val metadata: Map<String, Any?> = emptyMap(),
)
