package ai.run21.dispatchers.llm.ollama

import java.net.URI
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Verifies environment-driven Ollama configuration.
 *
 * These tests treat environment variables as an injectable map so configuration behavior stays
 * deterministic and does not depend on the developer machine.
 */
class OllamaLlmConfigTest {
    @Test
    fun `config factory reads environment-like values`() {
        val values = mapOf(
            "RUN21_LLM_BASE_URL" to "http://localhost:11434",
            "RUN21_LLM_MODEL" to "qwen2.5:1.5b",
            "RUN21_LLM_TIMEOUT_MS" to "45000",
            "RUN21_LLM_TEMPERATURE" to "0.2",
            "RUN21_DISPATCHER_DEBUG" to "true",
        )

        val config = OllamaLlmConfigFactory.fromEnvironment(values::get)

        assertEquals(URI.create("http://localhost:11434"), config.baseUrl)
        assertEquals("qwen2.5:1.5b", config.model)
        assertEquals(Duration.ofMillis(45_000), config.timeout)
        assertEquals(0.2, config.temperature)
        assertEquals(true, config.dispatcherDebug)
    }

    @Test
    fun `config rejects blank model and non positive timeout`() {
        assertFailsWith<IllegalArgumentException> {
            OllamaLlmConfig(model = "")
        }
        assertFailsWith<IllegalArgumentException> {
            OllamaLlmConfig(model = "qwen2.5:1.5b", timeout = Duration.ZERO)
        }
    }

    @Test
    fun `config factory fails when model environment value is missing`() {
        assertFailsWith<IllegalArgumentException> {
            OllamaLlmConfigFactory.fromEnvironment(emptyMap<String, String>()::get)
        }
    }

    @Test
    fun `config factory fails when model environment value is blank`() {
        assertFailsWith<IllegalArgumentException> {
            OllamaLlmConfigFactory.fromEnvironment(mapOf("RUN21_LLM_MODEL" to " ")::get)
        }
    }
}
