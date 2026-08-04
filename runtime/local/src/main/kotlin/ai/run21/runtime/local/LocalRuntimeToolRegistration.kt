package ai.run21.runtime.local

import ai.run21.core.execution.AuditMetadataFactory
import ai.run21.core.execution.DefaultToolExecutor
import ai.run21.core.execution.ToolExecutor
import ai.run21.core.model.ToolDescriptor
import ai.run21.core.registry.InMemoryToolImplementationRegistry
import ai.run21.tools.formula.EvalExFormulaToolImplementation
import ai.run21.tools.formula.FormulaToolDescriptorFactory
import ai.run21.tools.formula.FormulaToolRequestValidator

/**
 * Describes the tools and executors registered by a local runtime profile.
 */
data class LocalRuntimeToolRegistration(
    val tools: List<ToolDescriptor>,
    val executors: List<ToolExecutor>,
) {
    companion object {
        /**
         * Registers the formula evaluation tool with its EvalEx implementation.
         */
        fun formula(
            auditMetadataFactory: AuditMetadataFactory = AuditMetadataFactory(),
        ): LocalRuntimeToolRegistration {
            val descriptor = FormulaToolDescriptorFactory.create()
            val implementation = EvalExFormulaToolImplementation()
            val implementationRegistry = InMemoryToolImplementationRegistry(listOf(implementation))
            val executor = DefaultToolExecutor(
                descriptor = descriptor,
                implementationRegistry = implementationRegistry,
                validator = FormulaToolRequestValidator,
                auditMetadataFactory = auditMetadataFactory,
            )

            return LocalRuntimeToolRegistration(
                tools = listOf(descriptor),
                executors = listOf(executor),
            )
        }
    }
}
