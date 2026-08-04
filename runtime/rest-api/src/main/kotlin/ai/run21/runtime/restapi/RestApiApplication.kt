package ai.run21.runtime.restapi

import ai.run21.core.model.ToolCode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.ktor.http.ContentType
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
import io.ktor.server.response.respondText
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
            val readiness = runtime.readiness()
            call.respond(
                if (readiness.ready) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable,
                ReadyResponseDto(
                    status = if (readiness.ready) "ready" else "not_ready",
                    tools = readiness.tools,
                    executors = readiness.executors,
                    implementations = readiness.implementations,
                    checks = readiness.checks.map { check ->
                        ReadinessCheckDto(
                            name = check.name,
                            status = if (check.ready) "ready" else "not_ready",
                            details = check.details,
                        )
                    },
                ),
            )
        }

        get("/openapi.yaml") {
            call.respondText(
                text = openApiYaml(),
                contentType = ContentType.parse("application/yaml"),
            )
        }

        get("/docs") {
            call.respondText(
                text = swaggerUiHtml(),
                contentType = ContentType.Text.Html,
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

private fun openApiYaml(): String =
    requireNotNull(Thread.currentThread().contextClassLoader.getResource("openapi.yaml")) {
        "OpenAPI resource openapi.yaml was not found."
    }.readText()

private fun swaggerUiHtml(): String = """
    <!doctype html>
    <html lang="en">
    <head>
      <meta charset="utf-8">
      <title>Run21AI REST API</title>
      <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css">
    </head>
    <body>
      <div id="swagger-ui"></div>
      <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
      <script>
        window.onload = function() {
          window.ui = SwaggerUIBundle({
            url: "/openapi.yaml",
            dom_id: "#swagger-ui"
          });
        };
      </script>
    </body>
    </html>
""".trimIndent()
