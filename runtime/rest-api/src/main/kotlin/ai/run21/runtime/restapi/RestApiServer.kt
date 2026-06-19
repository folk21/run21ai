package ai.run21.runtime.restapi

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

/**
 * Starts the standalone Ktor server for local REST API usage.
 *
 * Deployment-specific modules can reuse [run21RestApi] directly; this main function is only the
 * simple local process entry point used by Gradle's `:runtime:rest-api:run` task.
 */
fun main() {
    embeddedServer(
        factory = Netty,
        port = System.getenv("RUN21_REST_PORT")?.toIntOrNull() ?: 8080,
        host = System.getenv("RUN21_REST_HOST") ?: "0.0.0.0",
    ) {
        run21RestApi()
    }.start(wait = true)
}
