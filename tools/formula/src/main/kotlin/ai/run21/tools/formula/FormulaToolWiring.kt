package ai.run21.tools.formula

import ai.run21.core.CoreToolRegistration
import ai.run21.core.CoreToolRuntime
import ai.run21.core.execution.AuditMetadataFactory
import ai.run21.core.execution.DefaultToolExecutor
import ai.run21.core.execution.ToolExecutionService
import ai.run21.core.registry.InMemoryToolImplementationRegistry
import java.time.Clock

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
     * executor, validator, and audit factory are assembled. Runtime modules should consume this
     * registration instead of rebuilding those parts themselves. Runtime assemblies that inject a
     * clock should pass an [AuditMetadataFactory] backed by the same clock used by the service.
     */
    fun registration(
        auditMetadataFactory: AuditMetadataFactory = AuditMetadataFactory(),
    ): CoreToolRegistration {
        val descriptor = FormulaToolDescriptorFactory.create()
        val implementation = EvalExFormulaToolImplementation()
        val implementationRegistry = InMemoryToolImplementationRegistry(
            implementations = listOf(implementation),
            defaultImplementations = mapOf(
                implementation.descriptor.toolCode to implementation.descriptor.implementationCode,
            ),
        )
        val executor = DefaultToolExecutor(
            descriptor = descriptor,
            implementationRegistry = implementationRegistry,
            validator = FormulaToolRequestValidator,
            auditMetadataFactory = auditMetadataFactory,
        )

        return CoreToolRegistration(
            tools = listOf(descriptor),
            executors = listOf(executor),
        )
    }

    /**
     * Creates a ready-to-use in-memory core runtime with only the formula tool registered and one shared clock.
     */
    fun runtime(
        clock: Clock = Clock.systemUTC(),
    ): CoreToolRuntime {
        val auditMetadataFactory = AuditMetadataFactory(clock)
        return CoreToolRuntime.inMemory(
            registration = registration(auditMetadataFactory),
            clock = clock,
        )
    }

    /**
     * Creates a ready-to-use in-memory service with only the formula tool registered and one shared clock.
     */
    fun service(
        clock: Clock = Clock.systemUTC(),
    ): ToolExecutionService = runtime(clock).executionService
}
