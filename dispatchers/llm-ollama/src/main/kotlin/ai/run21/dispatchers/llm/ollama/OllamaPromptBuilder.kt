package ai.run21.dispatchers.llm.ollama

import ai.run21.dispatchers.llm.LlmDispatchPrompt
import com.fasterxml.jackson.core.type.TypeReference
import org.stringtemplate.v4.ST

/**
 * Builds the instruction prompt sent to Ollama.
 *
 * The system and user prompt parts are externalized as StringTemplate resources so the chat request
 * structure stays visible: static dispatcher rules are separated from request-specific user input.
 */
object OllamaPromptBuilder {
    private const val SYSTEM_PROMPT_RESOURCE = "ollama/formula-dispatch/system-prompt.st"
    private const val USER_PROMPT_RESOURCE = "ollama/formula-dispatch/user-prompt.st"
    private const val OUTPUT_SCHEMA_RESOURCE = "ollama/formula-dispatch/output-schema.json"
    private const val TOOL_CODE = "formula.evaluate"
    private const val IMPLEMENTATION_CODE = "formula-evalex-v1"

    /**
     * Converts a provider-neutral dispatch prompt into chat messages expected by Ollama.
     */
    fun build(prompt: LlmDispatchPrompt): OllamaChatPrompt {
        val userPrompt = prompt.payload["prompt"]?.toString().orEmpty()
        return OllamaChatPrompt(
            messages = listOf(
                OllamaChatMessage(
                    role = "system",
                    content = render(
                        SYSTEM_PROMPT_RESOURCE,
                        mapOf(
                            "toolCode" to TOOL_CODE,
                            "implementationCode" to IMPLEMENTATION_CODE,
                        ),
                    ),
                ),
                OllamaChatMessage(
                    role = "user",
                    content = render(USER_PROMPT_RESOURCE, mapOf("userPrompt" to userPrompt)),
                ),
            ),
            format = OllamaJson.mapper.readValue(
                resourceText(OUTPUT_SCHEMA_RESOURCE),
                object : TypeReference<Map<String, Any?>>() {},
            ),
        )
    }

    private fun render(resourceName: String, values: Map<String, Any?>): String {
        val template = ST(resourceText(resourceName), '$', '$')
        values.forEach { (key, value) -> template.add(key, value) }
        return template.render()
    }

    private fun resourceText(resourceName: String): String =
        requireNotNull(Thread.currentThread().contextClassLoader.getResource(resourceName)) {
            "Ollama prompt resource $resourceName was not found."
        }.readText()
}

/**
 * Rendered chat prompt parts and structured-output schema sent to Ollama.
 */
data class OllamaChatPrompt(
    val messages: List<OllamaChatMessage>,
    val format: Map<String, Any?>,
)
