package ai.run21.dispatchers.llm.ollama

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.util.concurrent.CancellationException

/** Stable reasons reported when the configured Ollama dependency is not ready. */
enum class OllamaReadinessReason {
    UNAVAILABLE,
    TIMEOUT,
    HTTP_ERROR,
    INVALID_RESPONSE,
    MODEL_NOT_FOUND,
}

/** Result of checking Ollama reachability and configured model availability. */
data class OllamaReadinessResult(
    val ready: Boolean,
    val model: String,
    val reason: OllamaReadinessReason? = null,
)

/** Provider-level readiness boundary used by runtime modules. */
fun interface OllamaReadinessClient {
    suspend fun check(config: OllamaLlmConfig): OllamaReadinessResult
}

/** Asynchronous readiness client for Ollama's `/api/tags` endpoint. */
class JavaHttpOllamaReadinessClient(
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
) : OllamaReadinessClient {
    override suspend fun check(config: OllamaLlmConfig): OllamaReadinessResult {
        val endpoint = config.baseUrl.resolve(URI.create("/api/tags"))
        val request = HttpRequest.newBuilder(endpoint)
            .timeout(config.timeout)
            .GET()
            .build()

        return try {
            val response = httpClient
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .awaitCancellable()

            if (response.statusCode() !in 200..299) {
                return OllamaReadinessResult(
                    ready = false,
                    model = config.model,
                    reason = OllamaReadinessReason.HTTP_ERROR,
                )
            }

            val tags = try {
                OllamaJson.mapper.readValue(response.body(), OllamaTagsResponse::class.java)
            } catch (_: Exception) {
                return OllamaReadinessResult(
                    ready = false,
                    model = config.model,
                    reason = OllamaReadinessReason.INVALID_RESPONSE,
                )
            }

            val modelAvailable = tags.models.any { it.name == config.model || it.model == config.model }
            OllamaReadinessResult(
                ready = modelAvailable,
                model = config.model,
                reason = if (modelAvailable) null else OllamaReadinessReason.MODEL_NOT_FOUND,
            )
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: HttpTimeoutException) {
            OllamaReadinessResult(
                ready = false,
                model = config.model,
                reason = OllamaReadinessReason.TIMEOUT,
            )
        } catch (_: Exception) {
            OllamaReadinessResult(
                ready = false,
                model = config.model,
                reason = OllamaReadinessReason.UNAVAILABLE,
            )
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class OllamaTagsResponse(
    val models: List<OllamaModelInfo> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class OllamaModelInfo(
    val name: String? = null,
    val model: String? = null,
)
