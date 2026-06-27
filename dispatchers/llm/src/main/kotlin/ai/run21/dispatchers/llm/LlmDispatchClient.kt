package ai.run21.dispatchers.llm

/**
 * Provider-neutral client used by LlmDispatcher.
 *
 * Implementations may call a real LLM, a local model, a deterministic parser, or a fake test
 * double. The dispatcher depends only on this contract so tests do not need network access or API
 * credentials.
 */
fun interface LlmDispatchClient {
    suspend fun dispatch(prompt: LlmDispatchPrompt): LlmDispatchResponse
}
