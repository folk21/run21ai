package ai.run21.core.registry

import ai.run21.core.model.ToolCode
import ai.run21.core.model.ToolDescriptor

/**
 * Registry of tool capability descriptors.
 */
interface ToolRegistry {
    /**
     * Returns the descriptor for a registered tool capability.
     */
    fun find(code: ToolCode): ToolDescriptor?

    /**
     * Returns every known tool descriptor in registry order.
     */
    fun all(): List<ToolDescriptor>
}

/**
 * Lightweight in-memory tool descriptor registry.
 */
class InMemoryToolRegistry(
    descriptors: Iterable<ToolDescriptor> = emptyList(),
) : ToolRegistry {
    private val descriptorsByCode: MutableMap<ToolCode, ToolDescriptor> = linkedMapOf()

    init {
        descriptors.forEach(::register)
    }

    /**
     * Registers or replaces a tool descriptor by its tool code.
     */
    fun register(descriptor: ToolDescriptor) {
        descriptorsByCode[descriptor.code] = descriptor
    }

    override fun find(code: ToolCode): ToolDescriptor? = descriptorsByCode[code]

    override fun all(): List<ToolDescriptor> = descriptorsByCode.values.toList()
}
