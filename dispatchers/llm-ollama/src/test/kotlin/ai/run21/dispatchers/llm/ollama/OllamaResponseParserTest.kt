package ai.run21.dispatchers.llm.ollama

import ai.run21.dispatchers.llm.LlmDispatchAccepted
import ai.run21.dispatchers.llm.LlmDispatchRejected
import ai.run21.core.model.ToolErrorCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * Verifies parsing of model-generated structured output without calling a real model.
 *
 * Ollama returns text, and the model is instructed to put one JSON object in that text. These tests
 * protect the adapter boundary that converts that object into provider-neutral LlmDispatchResponse
 * values consumed by LlmDispatcher.
 */
class OllamaResponseParserTest {
    @Test
    fun `accept JSON response becomes accepted dispatch response`() {
        val response = OllamaResponseParser.parse(
            """
            {
              "decision": "accept",
              "targetTool": "formula.evaluate",
              "targetImplementation": "formula-evalex-v1",
              "inputPayload": {
                "expression": "a + b",
                "variables": {
                  "a": 10,
                  "b": 11
                }
              },
              "confidence": 0.92,
              "metadata": {
                "reason": "formula prompt"
              }
            }
            """.trimIndent(),
        )

        val accepted = assertIs<LlmDispatchAccepted>(response)
        assertEquals("formula.evaluate", accepted.targetTool)
        assertEquals("formula-evalex-v1", accepted.targetImplementation)
        assertEquals("a + b", accepted.inputPayload["expression"])
        assertEquals(0.92, accepted.confidence)
        assertEquals("ollama", accepted.metadata["provider"])
        assertEquals("formula prompt", accepted.metadata["reason"])
    }

    @Test
    fun `reject JSON response becomes rejected dispatch response`() {
        val response = OllamaResponseParser.parse(
            """
            ```json
            {
              "decision": "reject",
              "message": "Missing expression.",
              "details": {
                "missing": ["expression"]
              },
              "confidence": 0.3
            }
            ```
            """.trimIndent(),
        )

        val rejected = assertIs<LlmDispatchRejected>(response)
        assertEquals(ToolErrorCode.DISPATCH_REJECTED, rejected.errorCode)
        assertEquals("Missing expression.", rejected.message)
        assertEquals(listOf("expression"), rejected.details["missing"])
        assertEquals(0.3, rejected.confidence)
        assertEquals("ollama", rejected.metadata["provider"])
    }

    @Test
    fun `invalid text without JSON is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            OllamaResponseParser.parse("I cannot produce JSON.")
        }
    }

    @Test
    fun `accept response without target tool is rejected`() {
        assertFailsWith<IllegalArgumentException> {
            OllamaResponseParser.parse("""{"decision":"accept","inputPayload":{}}""")
        }
    }
}
