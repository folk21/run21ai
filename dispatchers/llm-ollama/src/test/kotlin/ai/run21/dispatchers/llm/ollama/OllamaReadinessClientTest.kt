package ai.run21.dispatchers.llm.ollama

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.net.Authenticator
import java.net.CookieHandler
import java.net.ProxySelector
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.time.Duration
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** Verifies Ollama dependency and configured-model readiness without a live server. */
class OllamaReadinessClientTest {
    @Test
    fun `readiness succeeds when configured model is installed`() = runBlocking {
        val httpClient = ReadinessHttpClient()
        val result = async(start = CoroutineStart.UNDISPATCHED) {
            JavaHttpOllamaReadinessClient(httpClient).check(testConfig())
        }

        assertEquals(URI.create("http://ollama.test:11434/api/tags"), httpClient.request?.uri())
        assertEquals("GET", httpClient.request?.method())
        httpClient.complete(
            200,
            """{"models":[{"name":"other:latest"},{"model":"qwen2.5:1.5b"}]}""",
        )

        assertEquals(OllamaReadinessResult(ready = true, model = "qwen2.5:1.5b"), result.await())
        assertFalse(httpClient.blockingSendCalled)
    }

    @Test
    fun `readiness reports missing configured model`() = runBlocking {
        val httpClient = ReadinessHttpClient()
        val result = async(start = CoroutineStart.UNDISPATCHED) {
            JavaHttpOllamaReadinessClient(httpClient).check(testConfig())
        }

        httpClient.complete(200, """{"models":[{"name":"other:latest"}]}""")

        assertEquals(
            OllamaReadinessResult(
                ready = false,
                model = "qwen2.5:1.5b",
                reason = OllamaReadinessReason.MODEL_NOT_FOUND,
            ),
            result.await(),
        )
    }

    @Test
    fun `readiness normalizes HTTP invalid response and timeout failures`() = runBlocking {
        suspend fun checkWith(completion: ReadinessHttpClient.() -> Unit): OllamaReadinessResult {
            val httpClient = ReadinessHttpClient()
            val result = async(start = CoroutineStart.UNDISPATCHED) {
                JavaHttpOllamaReadinessClient(httpClient).check(testConfig())
            }
            httpClient.completion()
            return result.await()
        }

        assertEquals(
            OllamaReadinessReason.HTTP_ERROR,
            checkWith { complete(503, "not-ready") }.reason,
        )
        assertEquals(
            OllamaReadinessReason.INVALID_RESPONSE,
            checkWith { complete(200, "not-json") }.reason,
        )
        assertEquals(
            OllamaReadinessReason.TIMEOUT,
            checkWith { fail(HttpTimeoutException("timed out")) }.reason,
        )
    }

    private fun testConfig(): OllamaLlmConfig = OllamaLlmConfig(
        model = "qwen2.5:1.5b",
        baseUrl = URI.create("http://ollama.test:11434"),
        timeout = Duration.ofSeconds(2),
    )
}

private class ReadinessHttpClient : HttpClient() {
    private val future = CompletableFuture<HttpResponse<String>>()
    var request: HttpRequest? = null
    var blockingSendCalled: Boolean = false

    fun complete(statusCode: Int, body: String) {
        future.complete(ReadinessHttpResponse(statusCode, body, requireNotNull(request)))
    }

    fun fail(error: Throwable) {
        future.completeExceptionally(error)
    }

    override fun cookieHandler(): Optional<CookieHandler> = Optional.empty()
    override fun connectTimeout(): Optional<Duration> = Optional.empty()
    override fun followRedirects(): Redirect = Redirect.NEVER
    override fun proxy(): Optional<ProxySelector> = Optional.empty()
    override fun sslContext(): SSLContext = SSLContext.getDefault()
    override fun sslParameters(): SSLParameters = SSLParameters()
    override fun authenticator(): Optional<Authenticator> = Optional.empty()
    override fun version(): Version = Version.HTTP_1_1
    override fun executor(): Optional<Executor> = Optional.empty()

    override fun <T : Any?> send(
        request: HttpRequest,
        responseBodyHandler: HttpResponse.BodyHandler<T>,
    ): HttpResponse<T> {
        blockingSendCalled = true
        error("Blocking HttpClient.send must not be used.")
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any?> sendAsync(
        request: HttpRequest,
        responseBodyHandler: HttpResponse.BodyHandler<T>,
    ): CompletableFuture<HttpResponse<T>> {
        this.request = request
        return future as CompletableFuture<HttpResponse<T>>
    }

    override fun <T : Any?> sendAsync(
        request: HttpRequest,
        responseBodyHandler: HttpResponse.BodyHandler<T>,
        pushPromiseHandler: HttpResponse.PushPromiseHandler<T>,
    ): CompletableFuture<HttpResponse<T>> = sendAsync(request, responseBodyHandler)
}

private class ReadinessHttpResponse<T>(
    private val statusCode: Int,
    private val body: T,
    private val request: HttpRequest,
) : HttpResponse<T> {
    override fun statusCode(): Int = statusCode
    override fun request(): HttpRequest = request
    override fun previousResponse(): Optional<HttpResponse<T>> = Optional.empty()
    override fun headers(): HttpHeaders = HttpHeaders.of(emptyMap()) { _, _ -> true }
    override fun body(): T = body
    override fun sslSession(): Optional<SSLSession> = Optional.empty()
    override fun uri(): URI = request.uri()
    override fun version(): HttpClient.Version = HttpClient.Version.HTTP_1_1
}
