package ai.run21.dispatchers.llm.ollama

import kotlinx.coroutines.CancellationException
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
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verifies non-blocking transport behavior, cancellation propagation, timeout preservation, and
 * redacted diagnostics without requiring a real Ollama server.
 */
class JavaHttpOllamaTransportTest {
    @Test
    fun `transport uses sendAsync and resumes after the HTTP future completes`() = runBlocking {
        val httpClient = ControlledHttpClient()
        val transport = JavaHttpOllamaTransport(httpClient)

        val result = async(start = CoroutineStart.UNDISPATCHED) {
            transport.chat(testRequest(), testConfig())
        }

        assertFalse(result.isCompleted)
        assertFalse(httpClient.blockingSendCalled)
        assertNotNull(httpClient.request)

        httpClient.complete(
            statusCode = 200,
            body = responseEnvelope("response-secret"),
        )

        assertEquals("response-secret", result.await().content)
        assertFalse(httpClient.blockingSendCalled)
    }

    @Test
    fun `caller cancellation cancels the underlying HTTP future`() = runBlocking {
        val httpClient = ControlledHttpClient()
        val transport = JavaHttpOllamaTransport(httpClient)

        val result = async(start = CoroutineStart.UNDISPATCHED) {
            transport.chat(testRequest(), testConfig())
        }

        result.cancel(CancellationException("Caller cancelled test request."))

        assertFailsWith<CancellationException> { result.await() }
        assertTrue(httpClient.responseFuture.isCancelled)
    }

    @Test
    fun `HTTP timeout remains a timeout failure`() = runBlocking {
        val httpClient = ControlledHttpClient()
        val transport = JavaHttpOllamaTransport(httpClient)

        val result = async(start = CoroutineStart.UNDISPATCHED) {
            transport.chat(testRequest(), testConfig())
        }

        httpClient.fail(HttpTimeoutException("HTTP request timed out with secret details."))

        assertFailsWith<HttpTimeoutException> { result.await() }
    }

    @Test
    fun `debug diagnostics expose metadata but not request or response payloads`() = runBlocking {
        val httpClient = ControlledHttpClient()
        val diagnostics = mutableListOf<String>()
        val transport = JavaHttpOllamaTransport(httpClient, diagnostics::add)

        val result = async(start = CoroutineStart.UNDISPATCHED) {
            transport.chat(
                request = testRequest(prompt = "request-secret-value"),
                config = testConfig(
                    dispatcherDebug = true,
                    baseUrl = URI.create("http://user:password@ollama.test:11434?token=query-secret"),
                ),
            )
        }

        httpClient.complete(
            statusCode = 200,
            body = responseEnvelope("response-secret-value"),
        )
        result.await()

        val output = diagnostics.joinToString("\n")
        assertContains(output, "method=POST")
        assertContains(output, "endpoint=http://ollama.test:11434/api/chat")
        assertContains(output, "model=qwen2.5:1.5b")
        assertContains(output, "messageCount=2")
        assertContains(output, "requestBytes=")
        assertContains(output, "status=200")
        assertContains(output, "responseBytes=")
        assertFalse(output.contains("request-secret-value"))
        assertFalse(output.contains("response-secret-value"))
        assertFalse(output.contains("user:password"))
        assertFalse(output.contains("query-secret"))
    }

    @Test
    fun `HTTP error does not expose response body in diagnostics or exception message`() = runBlocking {
        val httpClient = ControlledHttpClient()
        val diagnostics = mutableListOf<String>()
        val transport = JavaHttpOllamaTransport(httpClient, diagnostics::add)

        val result = async(start = CoroutineStart.UNDISPATCHED) {
            transport.chat(testRequest(), testConfig(dispatcherDebug = true))
        }

        httpClient.complete(
            statusCode = 500,
            body = "server-secret-response-body",
        )

        val error = assertFailsWith<IllegalStateException> { result.await() }
        assertEquals("Ollama request failed with HTTP status 500.", error.message)
        assertFalse(diagnostics.joinToString("\n").contains("server-secret-response-body"))
    }

    private fun testRequest(prompt: String = "Evaluate a + b."): OllamaChatRequest =
        OllamaChatRequest(
            model = "qwen2.5:1.5b",
            format = mapOf("type" to "object"),
            messages = listOf(
                OllamaChatMessage(role = "system", content = "Prepare a tool request."),
                OllamaChatMessage(role = "user", content = prompt),
            ),
        )

    private fun testConfig(
        dispatcherDebug: Boolean = false,
        baseUrl: URI = URI.create("http://ollama.test:11434"),
    ): OllamaLlmConfig =
        OllamaLlmConfig(
            model = "qwen2.5:1.5b",
            baseUrl = baseUrl,
            timeout = Duration.ofSeconds(5),
            dispatcherDebug = dispatcherDebug,
        )

    private fun responseEnvelope(content: String): String =
        """
        {
          "message": {
            "role": "assistant",
            "content": "$content"
          },
          "done": true
        }
        """.trimIndent()
}

private class ControlledHttpClient : HttpClient() {
    val responseFuture = CompletableFuture<HttpResponse<String>>()
    var request: HttpRequest? = null
    var blockingSendCalled: Boolean = false

    fun complete(statusCode: Int, body: String) {
        val capturedRequest = requireNotNull(request)
        responseFuture.complete(StubHttpResponse(statusCode, body, capturedRequest))
    }

    fun fail(error: Throwable) {
        responseFuture.completeExceptionally(error)
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
        return responseFuture as CompletableFuture<HttpResponse<T>>
    }

    override fun <T : Any?> sendAsync(
        request: HttpRequest,
        responseBodyHandler: HttpResponse.BodyHandler<T>,
        pushPromiseHandler: HttpResponse.PushPromiseHandler<T>,
    ): CompletableFuture<HttpResponse<T>> = sendAsync(request, responseBodyHandler)
}

private class StubHttpResponse<T>(
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
