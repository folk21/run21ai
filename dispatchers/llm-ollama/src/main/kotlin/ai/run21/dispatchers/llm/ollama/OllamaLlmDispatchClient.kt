package ai.run21.dispatchers.llm.ollama

import ai.run21.dispatchers.llm.LlmDispatchClient
import ai.run21.dispatchers.llm.LlmDispatchPrompt
import ai.run21.dispatchers.llm.LlmDispatchResponse

/**
 * LlmDispatchClient adapter for a locally or remotely running Ollama server.
 *
 * This class is intentionally only a provider adapter. It asks Ollama to prepare dispatcher JSON,
 * parses that JSON into provider-neutral `LlmDispatchResponse` values, and leaves actual tool work
 * to the normal Run21AI execution pipeline.
 */
class OllamaLlmDispatchClient(
    private val config: OllamaLlmConfig = OllamaLlmConfigFactory.fromEnvironment(),
    private val transport: OllamaTransport = JavaHttpOllamaTransport(),
) : LlmDispatchClient {
    /**
     * Sends a dispatcher prompt to Ollama and converts the model text into an LLM dispatch response.
     */
    override suspend fun dispatch(prompt: LlmDispatchPrompt): LlmDispatchResponse {
        val effectiveConfig = config.copy(
            dispatcherDebug = config.dispatcherDebug || prompt.attributes["dispatcherDebug"].toBooleanFlag(),
        )
        val chatPrompt = OllamaPromptBuilder.build(prompt)
        val request = OllamaChatRequest(
            model = effectiveConfig.model,
            stream = false,
            format = chatPrompt.format,
            options = mapOf("temperature" to effectiveConfig.temperature) + effectiveConfig.options,
            messages = chatPrompt.messages,
        )
        val response = transport.chat(request, effectiveConfig)
        val userPrompt = prompt.payload["prompt"]?.toString().orEmpty()
        val validatedJson = OllamaFormulaDispatchResponseValidator.validate(userPrompt, response.content)
        return OllamaResponseParser.parse(validatedJson)
    }

    private fun Any?.toBooleanFlag(): Boolean = when (this) {
        is Boolean -> this
        is String -> trim().lowercase() in setOf("1", "true", "yes", "on")
        else -> false
    }
}
