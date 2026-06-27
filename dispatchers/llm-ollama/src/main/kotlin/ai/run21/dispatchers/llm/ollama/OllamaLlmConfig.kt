package ai.run21.dispatchers.llm.ollama

import java.net.URI
import java.time.Duration

/**
 * Configuration for an Ollama-backed LLM dispatch client.
 *
 * The values describe only the model endpoint used for dispatching raw input into a `ToolRequest`.
 * They do not change tool execution semantics: accepted requests still flow through the core
 * dispatcher chain, executor, and tool implementation.
 */
data class OllamaLlmConfig(
    val model: String,
    val baseUrl: URI = URI.create("http://localhost:11434"),
    val timeout: Duration = Duration.ofSeconds(30),
    val temperature: Double = 0.0,
    val dispatcherDebug: Boolean = false,
    val options: Map<String, Any?> = emptyMap(),
) {
    init {
        require(model.isNotBlank()) { "Ollama model must not be blank." }
        require(!timeout.isNegative && !timeout.isZero) { "Ollama timeout must be positive." }
    }
}

/**
 * Loads Ollama LLM configuration from environment variables.
 *
 * The factory keeps deployment-specific configuration outside Gradle and source code. Tests can
 * pass a map-backed function, while the standalone REST server uses the real process environment.
 */
object OllamaLlmConfigFactory {
    /**
     * Creates an Ollama config from `RUN21_LLM_*` environment values.
     */
    fun fromEnvironment(
        env: (String) -> String? = System::getenv,
    ): OllamaLlmConfig = OllamaLlmConfig(
        model = requireNotNull(env("RUN21_LLM_MODEL")?.takeIf(String::isNotBlank)) {
            "RUN21_LLM_MODEL must be set to an installed Ollama model."
        },
        baseUrl = URI.create(env("RUN21_LLM_BASE_URL") ?: "http://localhost:11434"),
        timeout = Duration.ofMillis(env("RUN21_LLM_TIMEOUT_MS")?.toLongOrNull() ?: 30_000),
        temperature = env("RUN21_LLM_TEMPERATURE")?.toDoubleOrNull() ?: 0.0,
        dispatcherDebug = env("RUN21_DISPATCHER_DEBUG").toBooleanFlag(),
    )

    private fun String?.toBooleanFlag(): Boolean =
        this?.trim()?.lowercase() in setOf("1", "true", "yes", "on")
}
