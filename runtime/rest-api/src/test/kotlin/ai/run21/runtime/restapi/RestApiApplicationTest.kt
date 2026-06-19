package ai.run21.runtime.restapi

import ai.run21.tools.formula.FormulaTool
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Contract tests for the first Ktor REST API runtime.
 *
 * These tests exercise the HTTP boundary instead of calling core services directly. They verify
 * that routing, JSON mapping, runtime wiring, dispatcher-chain behavior, and tool execution remain
 * aligned with the core execution model.
 */
class RestApiApplicationTest {
    /**
     * Verifies that infrastructure endpoints are available without requiring a tool request.
     */
    @Test
    fun `health and readiness endpoints report local runtime state`() = testApplication {
        application {
            run21RestApi()
        }

        val health = client.get("/health")
        val ready = client.get("/ready")

        assertEquals(HttpStatusCode.OK, health.status)
        assertTrue(health.bodyAsText().contains("\"status\":\"ok\""))
        assertEquals(HttpStatusCode.OK, ready.status)
        assertTrue(ready.bodyAsText().contains("\"status\":\"ready\""))
    }

    /**
     * Verifies that the REST API exposes registered tool descriptors and their schemas.
     */
    @Test
    fun `tool endpoints expose formula descriptor and schemas`() = testApplication {
        application {
            run21RestApi()
        }
        val client = jsonClient()

        val toolsResponse = client.get("/v1/tools")
        val toolResponse = client.get("/v1/tools/${FormulaTool.TOOL_CODE.value}")

        assertEquals(HttpStatusCode.OK, toolsResponse.status)
        assertEquals(HttpStatusCode.OK, toolResponse.status)

        val tools = toolsResponse.body<List<ToolDescriptorDto>>()
        val tool = toolResponse.body<ToolDescriptorDto>()

        assertEquals(listOf(FormulaTool.TOOL_CODE.value), tools.map { it.code })
        assertEquals(FormulaTool.TOOL_CODE.value, tool.code)
        assertTrue(tool.inputSchema.isNotEmpty())
        assertTrue(tool.outputSchema.isNotEmpty())
    }

    /**
     * Verifies structured execution through the full HTTP-to-core pipeline.
     */
    @Test
    fun `executions endpoint runs structured formula request`() = testApplication {
        application {
            run21RestApi()
        }
        val client = jsonClient()

        val response = client.post("/v1/executions") {
            contentType(ContentType.Application.Json)
            setBody(formulaExecutionRequest())
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val result = response.body<ToolResultDto>()
        assertEquals("succeeded", result.status)
        assertEquals("21", result.outputPayload[FormulaTool.Output.VALUE].toString())
        assertEquals("pass-through", result.auditMetadata.dispatcherTrace.single().dispatcherCode)
        assertEquals("rest-api-test", result.auditMetadata.correlation.correlationId)
    }

    /**
     * Verifies that request resolution stops after dispatch and does not execute the formula tool.
     */
    @Test
    fun `resolve endpoint prepares structured request without executing it`() = testApplication {
        application {
            run21RestApi()
        }
        val client = jsonClient()

        val response = client.post("/v1/tool-requests/resolve") {
            contentType(ContentType.Application.Json)
            setBody(formulaExecutionRequest())
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val result = response.body<ResolveResponseDto>()
        assertEquals("accepted", result.status)
        assertEquals(FormulaTool.TOOL_CODE.value, result.request?.targetTool)
        assertEquals(null, result.error)
        assertEquals("pass-through", result.trace.single().dispatcherCode)
    }

    /**
     * Documents the current formula-only runtime behavior for prompt input.
     *
     * Prompt requests are valid HTTP input, but they require a raw dispatcher. Until that dispatcher
     * is registered, the pass-through-only chain must reject them in a structured way.
     */
    @Test
    fun `resolve endpoint rejects prompt request when no raw dispatcher is registered`() = testApplication {
        application {
            run21RestApi()
        }
        val client = jsonClient()

        val response = client.post("/v1/tool-requests/resolve") {
            contentType(ContentType.Application.Json)
            setBody(
                ExecutionRequestDto(
                    type = "prompt",
                    prompt = "calculate 10 + 11",
                    actor = testActor(),
                    source = testSource(),
                    correlation = CorrelationDto(correlationId = "prompt-resolve-test"),
                ),
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val result = response.body<ResolveResponseDto>()
        assertEquals("rejected", result.status)
        assertEquals("NO_DISPATCHER_ACCEPTED", result.error?.code)
        assertEquals("pass-through", result.trace.single().dispatcherCode)
    }

    /**
     * Creates a test client with the same JSON stack used by the server.
     */
    private fun ApplicationTestBuilder.jsonClient() = createClient {
        install(ContentNegotiation) {
            jackson {
                registerKotlinModule()
                registerModule(JavaTimeModule())
            }
        }
    }
}

private fun formulaExecutionRequest(): ExecutionRequestDto = ExecutionRequestDto(
    type = "structured",
    toolCode = FormulaTool.TOOL_CODE.value,
    implementationCode = FormulaTool.IMPLEMENTATION_CODE.value,
    input = mapOf(
        FormulaTool.Input.EXPRESSION to "a + b",
        FormulaTool.Input.VARIABLES to mapOf(
            "a" to BigDecimal("10"),
            "b" to BigDecimal("11"),
        ),
    ),
    actor = testActor(),
    source = testSource(),
    correlation = CorrelationDto(correlationId = "rest-api-test"),
)

private fun testActor(): ActorDto = ActorDto(
    id = "rest-api-test-actor",
    type = "human",
    displayName = "REST API Test Actor",
)

private fun testSource(): SourceDto = SourceDto(
    type = "api",
    name = "rest-api-test",
)
