package ai.run21.dispatchers.llm.ollama

import ai.run21.dispatchers.llm.LlmDispatchAccepted
import ai.run21.dispatchers.llm.LlmDispatchPrompt
import ai.run21.testsupport.runSuspend
import ai.run21.testsupport.testActor
import ai.run21.testsupport.testSource
import ai.run21.core.model.CorrelationMetadata
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Verifies OllamaLlmDispatchClient through a fake transport.
 *
 * This keeps unit tests independent from a real Ollama process while still checking request
 * construction, config usage, prompt generation, and parser integration.
 */
class OllamaLlmDispatchClientTest {
    @Test
    fun `client sends configured chat request and parses response`() {
        val transport = CapturingTransport(
            response = OllamaChatResponse(
                message = OllamaChatMessage(
                    role = "assistant",
                    content = """
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
                      "confidence": 0.88
                    }
                    """.trimIndent(),
                ),
                done = true,
            ),
        )
        val config = OllamaLlmConfig(
            baseUrl = URI.create("http://ollama.test:11434"),
            model = "qwen2.5:1.5b",
            temperature = 0.1,
            options = mapOf("top_p" to 0.9),
        )
        val client = OllamaLlmDispatchClient(config, transport)

        val response = runSuspend {
            client.dispatch(
                LlmDispatchPrompt(
                    payload = mapOf("prompt" to "Evaluate a + b with variables a = 10 and b = 11."),
                    actor = testActor(),
                    source = testSource(),
                    correlation = CorrelationMetadata(correlationId = "ollama-client-test"),
                ),
            )
        }

        val accepted = assertIs<LlmDispatchAccepted>(response)
        assertEquals("formula.evaluate", accepted.targetTool)
        assertEquals("a + b", accepted.inputPayload["expression"])
        assertEquals("qwen2.5:1.5b", transport.request?.model)
        assertEquals(false, transport.request?.stream)
        assertEquals(true, transport.request?.format?.isNotEmpty())
        assertEquals(0.1, transport.request?.options?.get("temperature"))
        assertEquals(0.9, transport.request?.options?.get("top_p"))
        assertEquals(URI.create("http://ollama.test:11434"), transport.config?.baseUrl)
        assertEquals(true, transport.request?.messages?.any { it.role == "system" })
        assertEquals(true, transport.request?.messages?.any { it.content.contains("Evaluate a + b") })
    }

    @Test
    fun `client enables dispatcher debug from prompt attributes`() {
        val transport = CapturingTransport(
            response = OllamaChatResponse(
                message = OllamaChatMessage(
                    role = "assistant",
                    content = """
                    {
                      "decision": "reject",
                      "targetTool": null,
                      "targetImplementation": null,
                      "inputPayload": {},
                      "confidence": 0.0,
                      "message": null,
                      "details": {
                        "reason": "ambiguous_request",
                        "missingVariables": []
                      },
                      "metadata": {}
                    }
                    """.trimIndent(),
                ),
                done = true,
            ),
        )
        val client = OllamaLlmDispatchClient(
            config = OllamaLlmConfig(model = "qwen2.5:1.5b"),
            transport = transport,
        )

        runSuspend {
            client.dispatch(
                LlmDispatchPrompt(
                    payload = mapOf("prompt" to "debug this dispatch"),
                    actor = testActor(),
                    source = testSource(),
                    correlation = CorrelationMetadata(correlationId = "ollama-debug-test"),
                    attributes = mapOf("dispatcherDebug" to true),
                ),
            )
        }

        assertEquals(true, transport.config?.dispatcherDebug)
    }

    private class CapturingTransport(
        private val response: OllamaChatResponse,
    ) : OllamaTransport {
        var request: OllamaChatRequest? = null
        var config: OllamaLlmConfig? = null

        override suspend fun chat(request: OllamaChatRequest, config: OllamaLlmConfig): OllamaChatResponse {
            this.request = request
            this.config = config
            return response
        }
    }
}
