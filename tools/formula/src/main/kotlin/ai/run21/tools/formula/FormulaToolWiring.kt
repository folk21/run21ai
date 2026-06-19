package ai.run21.tools.formula

import ai.run21.core.CoreDefaults
import ai.run21.core.execution.DefaultToolExecutionService
import ai.run21.core.execution.DefaultToolExecutor
import ai.run21.core.registry.InMemoryToolExecutorRegistry
import ai.run21.core.registry.InMemoryToolImplementationRegistry
import ai.run21.core.registry.InMemoryToolRegistry

/**
 * Lightweight local wiring helpers for the formula tool.
 *
 * Runtime modules can use these helpers for simple in-memory bootstrapping or replace them with
 * their own registration code.
 */
object FormulaToolWiring {
    /**
     * Creates a ready-to-use in-memory service with only the formula evaluation tool registered.
     */
    fun service(): DefaultToolExecutionService {
        val descriptor = FormulaToolDescriptorFactory.create()
        val implementation = EvalExFormulaToolImplementation()
        val implementationRegistry = InMemoryToolImplementationRegistry(listOf(implementation))
        val executor = DefaultToolExecutor(
            descriptor = descriptor,
            implementationRegistry = implementationRegistry,
            validator = FormulaToolRequestValidator,
        )

        return DefaultToolExecutionService(
            dispatcherChain = CoreDefaults.structuredDirectDispatcherChain(),
            toolRegistry = InMemoryToolRegistry(listOf(descriptor)),
            executorRegistry = InMemoryToolExecutorRegistry(listOf(executor)),
        )
    }
}
