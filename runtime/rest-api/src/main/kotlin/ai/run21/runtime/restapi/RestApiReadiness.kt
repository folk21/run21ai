package ai.run21.runtime.restapi

import ai.run21.core.CoreToolRuntimeAssembly
import ai.run21.dispatchers.llm.ollama.OllamaLlmConfig
import ai.run21.dispatchers.llm.ollama.OllamaReadinessClient
import java.util.concurrent.CancellationException

/** One readiness component result exposed by the REST runtime. */
data class RuntimeReadinessCheckResult(
    val name: String,
    val ready: Boolean,
    val details: Map<String, Any?> = emptyMap(),
)

/** Runtime dependency check executed by the readiness endpoint. */
interface RuntimeReadinessCheck {
    val name: String

    suspend fun check(): RuntimeReadinessCheckResult
}

/** Complete readiness report for registration state and runtime dependencies. */
data class RestApiReadinessReport(
    val ready: Boolean,
    val tools: Int,
    val executors: Int,
    val implementations: Int,
    val checks: List<RuntimeReadinessCheckResult>,
)

internal suspend fun CoreToolRuntimeAssembly.readinessReport(
    dependencyChecks: List<RuntimeReadinessCheck>,
): RestApiReadinessReport {
    val registryValidation = registryValidation()
    val registryCheck = RuntimeReadinessCheckResult(
        name = "registry-assembly",
        ready = registryValidation.valid,
        details = buildMap {
            put("tools", registryValidation.snapshot.toolCount)
            put("executors", registryValidation.snapshot.executorCount)
            put("implementations", registryValidation.snapshot.implementationCount)
            put(
                "defaultImplementations",
                registryValidation.snapshot.defaultImplementations.mapKeys { (toolCode, _) -> toolCode.value }
                    .mapValues { (_, implementationCode) -> implementationCode.value },
            )
            if (!registryValidation.valid) {
                put(
                    "issues",
                    registryValidation.issues.map { issue ->
                        buildMap {
                            put("code", issue.code.name)
                            issue.toolCode?.let { put("toolCode", it.value) }
                            issue.implementationCode?.let { put("implementationCode", it.value) }
                        }
                    },
                )
            }
        },
    )
    val checks = listOf(registryCheck) + dependencyChecks.map { check ->
        try {
            check.check()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            RuntimeReadinessCheckResult(
                name = check.name,
                ready = false,
                details = mapOf("reason" to "check_failed"),
            )
        }
    }

    return RestApiReadinessReport(
        ready = checks.all { it.ready },
        tools = registryValidation.snapshot.toolCount,
        executors = registryValidation.snapshot.executorCount,
        implementations = registryValidation.snapshot.implementationCount,
        checks = checks,
    )
}

/** Checks Ollama reachability and verifies that the configured model is installed. */
class OllamaRuntimeReadinessCheck(
    private val config: OllamaLlmConfig,
    private val client: OllamaReadinessClient,
) : RuntimeReadinessCheck {
    override val name: String = "ollama"

    override suspend fun check(): RuntimeReadinessCheckResult {
        val result = client.check(config)
        return RuntimeReadinessCheckResult(
            name = name,
            ready = result.ready,
            details = buildMap {
                put("model", result.model)
                result.reason?.let { put("reason", it.name.lowercase()) }
            },
        )
    }
}
