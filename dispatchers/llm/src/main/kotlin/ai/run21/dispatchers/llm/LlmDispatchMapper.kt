package ai.run21.dispatchers.llm

import ai.run21.core.model.ImplementationCode
import ai.run21.core.model.ToolCode
import ai.run21.core.model.ToolRequest

/**
 * Converts accepted LLM-style responses into canonical core ToolRequest values.
 */
object LlmDispatchMapper {
    fun toToolRequest(
        prompt: LlmDispatchPrompt,
        response: LlmDispatchAccepted,
    ): ToolRequest = ToolRequest(
        targetTool = ToolCode(response.targetTool),
        targetImplementation = response.targetImplementation?.let(::ImplementationCode),
        inputPayload = response.inputPayload,
        actor = prompt.actor,
        source = prompt.source,
        correlation = prompt.correlation,
        versionMetadata = response.versionMetadata,
        metadata = response.requestMetadata,
    )
}
