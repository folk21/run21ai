package ai.run21.runtime.restapi

import ai.run21.core.CoreToolRuntime
import ai.run21.core.CoreToolRuntimeAssembly
import ai.run21.core.dispatch.DispatcherChain
import ai.run21.core.dispatch.OrderedDispatcherChain
import ai.run21.core.dispatch.PassThroughDispatcher
import ai.run21.core.execution.AuditMetadataFactory
import ai.run21.core.execution.ToolExecutionService
import ai.run21.core.registry.ToolRegistry
import ai.run21.dispatchers.llm.LlmDispatcher
import ai.run21.dispatchers.llm.ollama.JavaHttpOllamaReadinessClient
import ai.run21.dispatchers.llm.ollama.OllamaLlmConfigFactory
import ai.run21.dispatchers.llm.ollama.OllamaLlmDispatchClient
import ai.run21.dispatchers.llm.ollama.OllamaReadinessClient
import ai.run21.tools.formula.FormulaToolWiring
import java.time.Clock

/** Runtime components required by the REST API layer. */
data class RestApiRuntime(
    val assembly: CoreToolRuntimeAssembly,
    val readinessChecks: List<RuntimeReadinessCheck> = emptyList(),
) {
    val core: CoreToolRuntime
        get() = assembly.runtime

    val dispatcherChain: DispatcherChain
        get() = assembly.dispatcherChain

    val toolRegistry: ToolRegistry
        get() = assembly.toolRegistry

    val service: ToolExecutionService
        get() = core.executionService

    /** Validates the current registration graph and all configured runtime dependencies. */
    suspend fun readiness(): RestApiReadinessReport =
        assembly.readinessReport(readinessChecks)
}

/** Creates REST API runtime profiles from code-based registrations. */
object RestApiRuntimeFactory {
    /**
     * Creates a REST runtime from environment configuration.
     *
     * `RUN21_REST_PROFILE=ollama` enables prompt dispatch through Ollama. The `llm-ollama` alias is
     * accepted for consistency with the Gradle module name. Any other value keeps the deterministic
     * formula-only profile.
     */
    fun fromEnvironment(
        env: (String) -> String? = System::getenv,
        clock: Clock = Clock.systemUTC(),
    ): RestApiRuntime = when (env("RUN21_REST_PROFILE")?.trim()?.lowercase()) {
        "ollama", "llm-ollama" -> formulaWithOllamaDispatcher(env, clock)
        else -> formulaOnly(clock)
    }

    /** Creates a formula-only REST runtime backed by a validated in-memory assembly. */
    fun formulaOnly(
        clock: Clock = Clock.systemUTC(),
    ): RestApiRuntime {
        val auditMetadataFactory = AuditMetadataFactory(clock)
        val assembly = CoreToolRuntime.inMemoryAssembly(
            registration = FormulaToolWiring.registration(auditMetadataFactory),
            clock = clock,
        )
        return RestApiRuntime(assembly)
    }

    /**
     * Creates a formula runtime with structured pass-through dispatch and Ollama-backed raw
     * dispatch. Readiness also verifies Ollama reachability and configured model availability.
     */
    fun formulaWithOllamaDispatcher(
        env: (String) -> String? = System::getenv,
        clock: Clock = Clock.systemUTC(),
        readinessClient: OllamaReadinessClient = JavaHttpOllamaReadinessClient(),
    ): RestApiRuntime {
        val config = OllamaLlmConfigFactory.fromEnvironment(env)
        val dispatcherChain = OrderedDispatcherChain(
            listOf(
                PassThroughDispatcher(),
                LlmDispatcher(client = OllamaLlmDispatchClient(config = config)),
            ),
        )
        val auditMetadataFactory = AuditMetadataFactory(clock)
        val assembly = CoreToolRuntime.inMemoryAssembly(
            registration = FormulaToolWiring.registration(auditMetadataFactory),
            dispatcherChain = dispatcherChain,
            clock = clock,
        )

        return RestApiRuntime(
            assembly = assembly,
            readinessChecks = listOf(
                OllamaRuntimeReadinessCheck(config, readinessClient),
            ),
        )
    }
}
