package ai.run21.tools.formula

import ai.run21.core.CoreToolRegistration
import ai.run21.core.CoreToolRuntime
import ai.run21.core.execution.DefaultToolExecutor
import ai.run21.core.execution.ToolExecutionService
import ai.run21.core.registry.InMemoryToolImplementationRegistry

/**
 * Lightweight local wiring helpers for the formula tool.
 *
 * Runtime modules can use these helpers for simple in-memory bootstrapping or replace them with
 * their own registration code.
 */
object FormulaToolWiring {
    /**
     * Creates the formula tool registration with its EvalEx implementation.
     *
     * This method is the single place where the formula descriptor, implementation registry,
     * executor, and validator are assembled. Runtime modules should consume this registration
     * instead of rebuilding those parts themselves.
     */
    fun registration(): CoreToolRegistration {
        val descriptor = FormulaToolDescriptorFactory.create()
        val implementation = EvalExFormulaToolImplementation()
        val implementationRegistry = InMemoryToolImplementationRegistry(listOf(implementation))
        val executor = DefaultToolExecutor(
            descriptor = descriptor,
            implementationRegistry = implementationRegistry,
            validator = FormulaToolRequestValidator,
        )

        return CoreToolRegistration(
            tools = listOf(descriptor),
            executors = listOf(executor),
        )
    }

    /**
     * Creates a ready-to-use in-memory core runtime with only the formula evaluation tool
     * registered.
     */
    fun runtime(): CoreToolRuntime =
        CoreToolRuntime.inMemory(registration())

    /**
     * Creates a ready-to-use in-memory service with only the formula evaluation tool registered.
     */
    fun service(): ToolExecutionService =
        runtime().executionService
}
