package ai.run21.dispatchers.llm.ollama

import ai.run21.dispatchers.llm.LlmDispatchPrompt
import ai.run21.testsupport.testActor
import ai.run21.testsupport.testSource
import ai.run21.core.model.CorrelationMetadata
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Verifies the instruction prompt sent to Ollama.
 *
 * The prompt must clearly constrain the model to dispatcher work: produce structured JSON, choose a
 * tool request, and avoid doing the actual tool calculation.
 */
class OllamaPromptBuilderTest {
    @Test
    fun `prompt instructs model to return dispatcher JSON only`() {
        val prompt = OllamaPromptBuilder.build(
            LlmDispatchPrompt(
                payload = mapOf("prompt" to "Calculate a + b where a=10 and b=11."),
                actor = testActor(),
                source = testSource(),
                correlation = CorrelationMetadata(correlationId = "prompt-builder-test"),
            ),
        )

        val systemPrompt = prompt.messages.first { it.role == "system" }.content
        val userPrompt = prompt.messages.first { it.role == "user" }.content

        assertTrue(systemPrompt.contains("Do not execute tools."))
        assertTrue(systemPrompt.contains("Do not calculate formula results."))
        assertTrue(systemPrompt.contains("The only supported tool is formula.evaluate."))
        assertTrue(systemPrompt.contains("never invent missing variable values"))
        assertTrue(userPrompt.contains("Calculate a + b where a=10 and b=11."))
        assertTrue(prompt.format.isNotEmpty())
    }
}
