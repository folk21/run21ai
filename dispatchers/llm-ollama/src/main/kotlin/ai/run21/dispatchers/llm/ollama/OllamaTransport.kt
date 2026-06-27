package ai.run21.dispatchers.llm.ollama

/**
 * Small transport boundary for the Ollama chat endpoint.
 *
 * Production uses Java HttpClient; unit tests provide an in-memory implementation that captures the
 * generated request and returns a deterministic response.
 */
fun interface OllamaTransport {
    /**
     * Sends a chat request using the supplied runtime configuration.
     */
    fun chat(request: OllamaChatRequest, config: OllamaLlmConfig): OllamaChatResponse
}
