package ai.run21.runtime.restapi

import ai.run21.core.CoreDefaults
import ai.run21.core.dispatch.DispatcherChain
import ai.run21.core.execution.DefaultToolExecutionService
import ai.run21.core.execution.ToolExecutionService
import ai.run21.core.registry.InMemoryToolExecutorRegistry
import ai.run21.core.registry.InMemoryToolRegistry
import ai.run21.core.registry.ToolRegistry
import ai.run21.runtime.local.LocalRuntimeToolRegistration

/**
 * Runtime components required by the REST API layer.
 *
 * The REST module keeps these references together because different endpoints need different
 * parts of the runtime: execution uses the service, request resolution uses the dispatcher chain,
 * and discovery/readiness use the tool registry.
 */
data class RestApiRuntime(
    val service: ToolExecutionService,
    val dispatcherChain: DispatcherChain,
    val toolRegistry: ToolRegistry,
) {
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
     * Creates a formula-only REST runtime backed by in-memory registries.
     *
     * This is the first REST profile. It proves Ktor can expose the existing core pipeline without
     * adding HTTP concerns to `core` or tool-specific execution paths to the runtime.
     */
    fun formulaOnly(): RestApiRuntime {
        val registration = LocalRuntimeToolRegistration.formula()
        val dispatcherChain = CoreDefaults.structuredDirectDispatcherChain()
        val toolRegistry = InMemoryToolRegistry(registration.tools)
        val executorRegistry = InMemoryToolExecutorRegistry(registration.executors)
        val service = DefaultToolExecutionService(
            dispatcherChain = dispatcherChain,
            toolRegistry = toolRegistry,
            executorRegistry = executorRegistry,
        )

        return RestApiRuntime(
            service = service,
            dispatcherChain = dispatcherChain,
            toolRegistry = toolRegistry,
        )
    }
}
