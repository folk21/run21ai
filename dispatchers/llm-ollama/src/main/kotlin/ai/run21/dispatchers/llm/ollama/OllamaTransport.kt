package ai.run21.dispatchers.llm.ollama

/**
 * Asynchronous transport boundary for the Ollama chat endpoint.
 *
 * Production uses Java `HttpClient.sendAsync`; tests can provide a deterministic in-memory
 * implementation without starting an Ollama process.
 */
fun interface OllamaTransport {
    /**
     * Sends a chat request without blocking the caller thread.
     *
     * Caller cancellation should cancel the underlying transport operation when the implementation
     * supports it. Timeout failures must remain distinguishable from caller cancellation.
     */
    suspend fun chat(request: OllamaChatRequest, config: OllamaLlmConfig): OllamaChatResponse
}
