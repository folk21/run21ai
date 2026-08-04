package ai.run21.core.registry

import ai.run21.core.execution.ToolImplementation
import ai.run21.core.model.ImplementationCode
import ai.run21.core.model.ToolCode

/**
 * Registry that resolves concrete implementations for tool execution.
 *
 * Exact implementation selection uses the requested implementation code. Calls without an
 * implementation code use only an explicitly registered default; registration order is never a
 * selection policy.
 */
interface ToolImplementationRegistry {
    fun find(toolCode: ToolCode, implementationCode: ImplementationCode? = null): ToolImplementation?

    fun defaultImplementation(toolCode: ToolCode): ImplementationCode? = null

    fun all(): List<ToolImplementation>

    fun allFor(toolCode: ToolCode): List<ToolImplementation>
}

/**
 * Fail-fast in-memory implementation registry for tests and code-based runtime assembly.
 */
class InMemoryToolImplementationRegistry(
    implementations: Iterable<ToolImplementation> = emptyList(),
    defaultImplementations: Map<ToolCode, ImplementationCode> = emptyMap(),
) : ToolImplementationRegistry {
    private val implementationsByToolCode: MutableMap<ToolCode, LinkedHashMap<ImplementationCode, ToolImplementation>> =
        linkedMapOf()
    private val defaultsByToolCode: MutableMap<ToolCode, ImplementationCode> = linkedMapOf()

    init {
        implementations.forEach(::register)
        defaultImplementations.forEach(::registerDefault)
    }

    /** Registers a new implementation and rejects duplicate tool/implementation pairs. */
    fun register(implementation: ToolImplementation) {
        val descriptor = implementation.descriptor
        val toolImplementations = implementationsByToolCode.getOrPut(descriptor.toolCode) { linkedMapOf() }
        require(descriptor.implementationCode !in toolImplementations) {
            "Duplicate tool implementation registration: " +
                "${descriptor.toolCode.value}/${descriptor.implementationCode.value}."
        }
        toolImplementations[descriptor.implementationCode] = implementation
    }

    /** Registers the only default implementation for a tool. */
    fun registerDefault(toolCode: ToolCode, implementationCode: ImplementationCode) {
        require(toolCode !in defaultsByToolCode) {
            "Default implementation is already registered for tool ${toolCode.value}."
        }
        require(implementationsByToolCode[toolCode]?.containsKey(implementationCode) == true) {
            "Default implementation ${implementationCode.value} is not registered for tool ${toolCode.value}."
        }
        defaultsByToolCode[toolCode] = implementationCode
    }

    override fun find(toolCode: ToolCode, implementationCode: ImplementationCode?): ToolImplementation? {
        val toolImplementations = implementationsByToolCode[toolCode] ?: return null
        val resolvedCode = implementationCode ?: defaultsByToolCode[toolCode] ?: return null
        return toolImplementations[resolvedCode]
    }

    override fun defaultImplementation(toolCode: ToolCode): ImplementationCode? = defaultsByToolCode[toolCode]

    override fun all(): List<ToolImplementation> =
        implementationsByToolCode.values.flatMap { it.values }

    override fun allFor(toolCode: ToolCode): List<ToolImplementation> =
        implementationsByToolCode[toolCode]?.values?.toList().orEmpty()
}
