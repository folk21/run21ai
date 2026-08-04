package ai.run21.runtime.restapi

import ai.run21.core.CoreToolRuntime
import ai.run21.core.dispatch.DispatcherChain
import ai.run21.core.dispatch.OrderedDispatcherChain
import ai.run21.core.dispatch.PassThroughDispatcher
import ai.run21.core.execution.AuditMetadataFactory
import ai.run21.core.execution.ToolExecutionService
import ai.run21.core.registry.ToolRegistry
import ai.run21.dispatchers.llm.LlmDispatcher
import ai.run21.dispatchers.llm.ollama.OllamaLlmConfigFactory
import ai.run21.dispatchers.llm.ollama.OllamaLlmDispatchClient
import ai.run21.tools.formula.FormulaToolWiring
import java.time.Clock

/**
 * Runtime components required by the REST API layer.
 *
 * The REST module keeps these references together because different endpoints need different
 * parts of the runtime: execution uses the service, request resolution uses the dispatcher chain,
 * and discovery/readiness use the tool registry.
 */
data class RestApiRuntime(
    val core: CoreToolRuntime,
    val dispatcherChain: DispatcherChain,
    val toolRegistry: ToolRegistry,
) {
    val service: ToolExecutionService
        get() = core.executionService

    /**
     * Reports whether the API has enough runtime wiring to accept traffic.
     */
    fun isReady(): Boolean = toolRegistry.all().isNotEmpty()
}

/**
 * Creates REST API runtime profiles from code-based local registrations.
 */
object RestApiRuntimeFactory {
    /**
     * Creates a REST runtime from environment configuration.
     *
     * `RUN21_REST_PROFILE=ollama` enables prompt dispatch through a local Ollama server. The
     * `llm-ollama` alias is accepted for consistency with the Gradle module name. Any other value
     * keeps the deterministic formula-only profile.
     */
    fun fromEnvironment(
        env: (String) -> String? = System::getenv,
        clock: Clock = Clock.systemUTC(),
    ): RestApiRuntime = when (env("RUN21_REST_PROFILE")?.trim()?.lowercase()) {
        "ollama", "llm-ollama" -> formulaWithOllamaDispatcher(env, clock)
        else -> formulaOnly(clock)
    }

    /**
     * Creates a formula-only REST runtime backed by in-memory registries.
     *
     * The service and formula executor share the supplied clock so audit timestamps are
     * deterministic when a fixed clock is injected.
     */
    fun formulaOnly(
        clock: Clock = Clock.systemUTC(),
    ): RestApiRuntime {
        val auditMetadataFactory = AuditMetadataFactory(clock)
        val assembly = CoreToolRuntime.inMemoryAssembly(
            registration = FormulaToolWiring.registration(auditMetadataFactory),
            clock = clock,
        )

        return RestApiRuntime(
            core = assembly.runtime,
            dispatcherChain = assembly.dispatcherChain,
            toolRegistry = assembly.toolRegistry,
        )
    }

    /**
     * Creates a formula runtime with pass-through structured dispatch and Ollama-backed raw
     * dispatch for prompt-like requests.
     */
    fun formulaWithOllamaDispatcher(
        env: (String) -> String? = System::getenv,
        clock: Clock = Clock.systemUTC(),
    ): RestApiRuntime {
        val dispatcherChain = OrderedDispatcherChain(
            listOf(
                PassThroughDispatcher(),
                LlmDispatcher(
                    client = OllamaLlmDispatchClient(
                        config = OllamaLlmConfigFactory.fromEnvironment(env),
                    ),
                ),
            ),
        )
        val auditMetadataFactory = AuditMetadataFactory(clock)
        val assembly = CoreToolRuntime.inMemoryAssembly(
            registration = FormulaToolWiring.registration(auditMetadataFactory),
            dispatcherChain = dispatcherChain,
            clock = clock,
        )

        return RestApiRuntime(
            core = assembly.runtime,
            dispatcherChain = assembly.dispatcherChain,
            toolRegistry = assembly.toolRegistry,
        )
    }
}
