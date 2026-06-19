package ai.run21.runtime.restapi

import ai.run21.core.model.ToolCode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

/**
 * Installs the Run21AI REST API into a Ktor application.
 *
 * The function accepts a prebuilt runtime so tests and future deployment modules can supply their
 * own tool registrations or dispatcher-chain profiles without changing routing code.
 */
fun Application.run21RestApi(runtime: RestApiRuntime = RestApiRuntimeFactory.formulaOnly()) {
    install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
            registerModule(JavaTimeModule())
        }
    }
    install(StatusPages) {
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ToolErrorDto(
                    code = "INVALID_HTTP_REQUEST",
                    message = cause.message ?: "Invalid request.",
                ),
            )
        }
        exception<BadRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ToolErrorDto(
                    code = "INVALID_HTTP_REQUEST",
                    message = cause.message ?: "Invalid request body.",
                ),
            )
        }
    }

    routing {
        get("/health") {
            call.respond(HealthResponseDto(status = "ok"))
        }

        get("/ready") {
            val status = if (runtime.isReady()) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
            call.respond(
                status,
                ReadyResponseDto(
                    status = if (runtime.isReady()) "ready" else "not_ready",
                    tools = runtime.toolRegistry.all().size,
                ),
            )
        }

        route("/v1") {
            get("/tools") {
                call.respond(runtime.toolRegistry.all().map { it.toDto() })
            }

            get("/tools/{code}") {
                val code = call.parameters["code"].orEmpty()
                val descriptor = runtime.toolRegistry.find(ToolCode(code))
                if (descriptor == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ToolErrorDto(
                            code = "UNKNOWN_TOOL",
                            message = "Target tool is not registered.",
                            details = mapOf("toolCode" to code),
                        ),
                    )
                } else {
                    call.respond(descriptor.toDto())
                }
            }

            post("/tool-requests/resolve") {
                val request = call.receiveExecutionRequest()
                val result = runtime.dispatcherChain.dispatch(request.toDispatchInput())
                call.respond(result.toDto())
            }

            post("/executions") {
                val request = call.receiveExecutionRequest()
                val result = runtime.service.execute(request.toDispatchInput())
                call.respond(result.toDto())
            }
        }
    }
}

/**
 * Reads the shared execution request body used by resolve and execute endpoints.
 */
private suspend fun ApplicationCall.receiveExecutionRequest(): ExecutionRequestDto =
    receive()
