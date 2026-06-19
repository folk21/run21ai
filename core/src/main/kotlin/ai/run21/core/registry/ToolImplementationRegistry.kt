package ai.run21.core.registry

import ai.run21.core.execution.ToolImplementation
import ai.run21.core.model.ImplementationCode
import ai.run21.core.model.ToolCode

/**
 * Registry that resolves concrete implementations for tool execution.
 *
 * When an implementation code is provided, the registry must resolve that exact
 * implementation. When it is omitted, the registry may return the default implementation
 * for the target tool.
 */
interface ToolImplementationRegistry {
    fun find(toolCode: ToolCode, implementationCode: ImplementationCode? = null): ToolImplementation?

    fun all(): List<ToolImplementation>

    fun allFor(toolCode: ToolCode): List<ToolImplementation>
}

/**
 * Lightweight in-memory implementation registry used by contract tests and simple
 * local setups. Runtime modules can replace it with persistence-backed registries.
 */
class InMemoryToolImplementationRegistry(
    implementations: Iterable<ToolImplementation> = emptyList(),
) : ToolImplementationRegistry {
    private val implementationsByToolCode: MutableMap<ToolCode, LinkedHashMap<ImplementationCode, ToolImplementation>> =
        linkedMapOf()

    init {
        implementations.forEach(::register)
    }

    /**
     * Registers or replaces an implementation for its declared tool and implementation code.
     */
    fun register(implementation: ToolImplementation) {
        val descriptor = implementation.descriptor
        val toolImplementations = implementationsByToolCode.getOrPut(descriptor.toolCode) { linkedMapOf() }
        toolImplementations[descriptor.implementationCode] = implementation
    }

    /**
     * Resolves either the requested implementation or the first registered implementation for a tool.
     */
    override fun find(toolCode: ToolCode, implementationCode: ImplementationCode?): ToolImplementation? {
        val toolImplementations = implementationsByToolCode[toolCode] ?: return null
        return if (implementationCode == null) {
            // The first registered implementation is the in-memory default. Runtime registries may
            // apply explicit defaulting rules.
            toolImplementations.values.firstOrNull()
        } else {
            toolImplementations[implementationCode]
        }
    }

    override fun all(): List<ToolImplementation> =
        implementationsByToolCode.values.flatMap { it.values }

    override fun allFor(toolCode: ToolCode): List<ToolImplementation> =
        implementationsByToolCode[toolCode]?.values?.toList().orEmpty()
}
