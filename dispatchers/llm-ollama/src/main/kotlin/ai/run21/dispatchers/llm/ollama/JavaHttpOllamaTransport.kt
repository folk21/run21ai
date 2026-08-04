package ai.run21.dispatchers.llm.ollama

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

/**
 * Asynchronous Java HttpClient transport for Ollama's `/api/chat` endpoint.
 *
 * Debug diagnostics contain operational metadata only. Prompt text, serialized request bodies,
 * response bodies, URI credentials, query parameters, and exception messages are never logged.
 */
class JavaHttpOllamaTransport(
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
    private val debugSink: (String) -> Unit = { message -> System.err.println(message) },
) : OllamaTransport {
    override suspend fun chat(request: OllamaChatRequest, config: OllamaLlmConfig): OllamaChatResponse {
        val endpoint = config.baseUrl.resolve(URI.create("/api/chat"))
        val body = OllamaJson.mapper.writeValueAsString(request)
        val httpRequest = HttpRequest.newBuilder(endpoint)
            .timeout(config.timeout)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        if (config.dispatcherDebug) {
            debugSink(
                "RUN21 dispatcher debug: Ollama request " +
                    "method=POST endpoint=${endpoint.redactedForDiagnostics()} " +
                    "model=${request.model} messageCount=${request.messages.size} " +
                    "requestBytes=${body.utf8Size()}",
            )
        }

        val response = try {
            httpClient
                .sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                .awaitCancellable()
        } catch (failure: Throwable) {
            if (config.dispatcherDebug) {
                debugSink(
                    "RUN21 dispatcher debug: Ollama request " +
                        "outcome=failed failureType=${failure.redactedTypeName()}",
                )
            }
            throw failure
        }

        val responseBody = response.body()
        if (config.dispatcherDebug) {
            debugSink(
                "RUN21 dispatcher debug: Ollama response " +
                    "status=${response.statusCode()} responseBytes=${responseBody.utf8Size()}",
            )
        }

        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("Ollama request failed with HTTP status ${response.statusCode()}.")
        }

        return try {
            OllamaJson.mapper.readValue(responseBody, OllamaChatResponse::class.java)
        } catch (_: Exception) {
            throw IllegalStateException("Ollama returned an invalid response envelope.")
        }
    }
}

private fun Throwable.redactedTypeName(): String =
    unwrapCompletionFailure().javaClass.simpleName.ifBlank { "UnknownFailure" }

private fun URI.redactedForDiagnostics(): String = buildString {
    if (!scheme.isNullOrBlank()) {
        append(scheme)
        append("://")
    }
    append(host ?: "unknown-host")
    if (port >= 0) {
        append(':')
        append(port)
    }
    append(path?.takeIf(String::isNotBlank) ?: "/")
}

private fun String.utf8Size(): Int = toByteArray(StandardCharsets.UTF_8).size
