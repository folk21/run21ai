package ai.run21.dispatchers.llm.ollama

import ai.run21.dispatchers.llm.LlmDispatchAccepted
import ai.run21.dispatchers.llm.LlmDispatchRejected
import ai.run21.dispatchers.llm.LlmDispatchResponse
import ai.run21.core.model.ToolErrorCode

/**
 * Parses the model-generated JSON object from Ollama's text response.
 *
 * Ollama's generate API returns text, so the adapter boundary must be defensive: extract a JSON
 * object from plain or fenced text, reject malformed accepted responses, and keep the result in the
 * provider-neutral LLM dispatcher model.
 */
object OllamaResponseParser {
    /**
     * Converts model text into an accepted or rejected dispatcher response.
     */
    fun parse(responseText: String): LlmDispatchResponse {
        val json = extractJsonObject(responseText)
        val parsed = OllamaJson.mapper.readValue(json, OllamaDispatchJson::class.java)
        return when (parsed.decision?.lowercase()) {
            "accept" -> accepted(parsed)
            "reject" -> rejected(parsed)
            else -> throw IllegalArgumentException("Ollama dispatch response must contain decision accept or reject.")
        }
    }

    private fun accepted(parsed: OllamaDispatchJson): LlmDispatchAccepted {
        val targetTool = parsed.targetTool?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("Accepted Ollama dispatch response must contain targetTool.")
        return LlmDispatchAccepted(
            targetTool = targetTool,
            targetImplementation = parsed.targetImplementation?.takeIf(String::isNotBlank),
            inputPayload = parsed.inputPayload,
            confidence = parsed.confidence,
            inputSummary = "Ollama generated an accepted dispatcher response.",
            outputSummary = "Prepared $targetTool request.",
            metadata = parsed.metadata + ("provider" to "ollama"),
        )
    }

    private fun rejected(parsed: OllamaDispatchJson): LlmDispatchRejected = LlmDispatchRejected(
        message = parsed.message?.takeIf(String::isNotBlank) ?: "Ollama rejected the prompt.",
        errorCode = ToolErrorCode.DISPATCH_REJECTED,
        details = parsed.details,
        confidence = parsed.confidence,
        inputSummary = "Ollama generated a rejected dispatcher response.",
        outputSummary = parsed.message,
        metadata = parsed.metadata + ("provider" to "ollama"),
    )

    private fun extractJsonObject(text: String): String {
        val trimmed = text.trim()
        val fenced = Regex("```(?:json)?\\s*([\\s\\S]*?)\\s*```").find(trimmed)?.groupValues?.get(1)
        val candidate = fenced?.trim() ?: trimmed
        val start = candidate.indexOf('{')
        val end = candidate.lastIndexOf('}')
        if (start < 0 || end < start) {
            throw IllegalArgumentException("Ollama response did not contain a JSON object.")
        }
        return candidate.substring(start, end + 1)
    }
}
