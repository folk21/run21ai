package ai.run21.dispatchers.llm.ollama

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Java HttpClient-based transport for Ollama's /api/chat endpoint.
 *
 * The transport is separated from `OllamaLlmDispatchClient` so unit tests can verify client
 * behavior with a fake transport and without starting a real Ollama process.
 */
class JavaHttpOllamaTransport(
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
) : OllamaTransport {
    /**
     * Sends one non-streaming chat request and parses Ollama's JSON response envelope.
     */
    override fun chat(request: OllamaChatRequest, config: OllamaLlmConfig): OllamaChatResponse {
        val endpoint = config.baseUrl.resolve(URI.create("/api/chat"))
        val body = OllamaJson.mapper.writeValueAsString(request)
        if (config.dispatcherDebug) {
            System.err.println("RUN21 dispatcher debug: Ollama request POST $endpoint")
            System.err.println(body)
        }
        val httpRequest = HttpRequest.newBuilder(endpoint)
            .timeout(config.timeout)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        val response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString())
        if (config.dispatcherDebug) {
            System.err.println("RUN21 dispatcher debug: Ollama response HTTP ${response.statusCode()}")
            System.err.println(response.body())
        }
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("Ollama returned HTTP ${response.statusCode()}: ${response.body()}")
        }
        return OllamaJson.mapper.readValue(response.body(), OllamaChatResponse::class.java)
    }
}
