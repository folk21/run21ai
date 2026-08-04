package ai.run21.runtime.restapi

import ai.run21.dispatchers.llm.ollama.OllamaReadinessClient
import ai.run21.dispatchers.llm.ollama.OllamaReadinessReason
import ai.run21.dispatchers.llm.ollama.OllamaReadinessResult
import ai.run21.testsupport.runSuspend
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Verifies registry and Ollama dependency readiness composition. */
class RestApiReadinessTest {
    @Test
    fun `formula-only runtime is ready with a complete registration graph`() {
        val readiness = runSuspend { RestApiRuntimeFactory.formulaOnly().readiness() }

        assertTrue(readiness.ready)
        assertEquals(1, readiness.tools)
        assertEquals(1, readiness.executors)
        assertEquals(1, readiness.implementations)
        val registry = readiness.checks.single { it.name == "registry-assembly" }
        assertTrue(registry.ready)
        assertEquals(
            mapOf("formula.evaluate" to "formula-evalex-v1"),
            registry.details["defaultImplementations"],
        )
    }

    @Test
    fun `ollama runtime is not ready when configured model is unavailable`() {
        val readinessClient = OllamaReadinessClient { config ->
            OllamaReadinessResult(
                ready = false,
                model = config.model,
                reason = OllamaReadinessReason.MODEL_NOT_FOUND,
            )
        }
        val runtime = RestApiRuntimeFactory.formulaWithOllamaDispatcher(
            env = mapOf(
                "RUN21_LLM_MODEL" to "qwen2.5:1.5b",
                "RUN21_LLM_BASE_URL" to "http://ollama.test:11434",
            )::get,
            readinessClient = readinessClient,
        )

        val readiness = runSuspend { runtime.readiness() }

        assertFalse(readiness.ready)
        val ollama = readiness.checks.single { it.name == "ollama" }
        assertFalse(ollama.ready)
        assertEquals("qwen2.5:1.5b", ollama.details["model"])
        assertEquals("model_not_found", ollama.details["reason"])
    }
}
