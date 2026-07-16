package ai.run21.runtime.local

import ai.run21.core.CoreDefaults
import ai.run21.core.CoreToolRuntime
import ai.run21.core.execution.DefaultToolExecutionService
import ai.run21.core.registry.InMemoryToolExecutorRegistry
import ai.run21.core.registry.InMemoryToolImplementationRegistry
import ai.run21.core.registry.InMemoryToolRegistry

/**
 * Creates local in-memory runtime instances.
 */
object LocalRun21RuntimeFactory {
    /**
     * Creates a runtime with the formula evaluation tool registered.
     */
    fun formulaOnly(): LocalRun21Runtime {
        val registration = LocalRuntimeToolRegistration.formula()

        val service = DefaultToolExecutionService(
            dispatcherChain = CoreDefaults.structuredDirectDispatcherChain(),
            toolRegistry = InMemoryToolRegistry(registration.tools),
            executorRegistry = InMemoryToolExecutorRegistry(registration.executors),
        )

        return LocalRun21Runtime(CoreToolRuntime(service))
    }
}
