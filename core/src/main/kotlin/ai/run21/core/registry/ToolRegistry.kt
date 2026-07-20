package ai.run21.core.registry

import ai.run21.core.model.ToolCode
import ai.run21.core.model.ToolDescriptor

/**
 * Registry of declared tool capabilities.
 *
 * `ToolRegistry` stores [ToolDescriptor] metadata: tool code, display information, input/output
 * contracts, status, and other capability-level facts that can be used for discovery,
 * validation, readiness checks, or API documentation.
 *
 * This registry does not execute tools and does not know which concrete implementation will run.
 * Execution routing is handled separately by `ToolExecutorRegistry`, and implementation selection
 * is handled by `ToolImplementationRegistry`.
 *
 * In the normal execution path, `DefaultToolExecutionService` first uses this registry to verify
 * that the requested tool is a known capability. Only after that does it resolve a `ToolExecutor`
 * for the same tool code.
 */
interface ToolRegistry {
    /**
     * Returns the descriptor for a registered tool capability.
     *
     * A missing descriptor means the tool code is unknown to this runtime. The execution service
     * should treat that as `UNKNOWN_TOOL` before trying to resolve an executor.
     */
    fun find(code: ToolCode): ToolDescriptor?

    /**
     * Returns every known tool descriptor in registry order.
     *
     * This is intended for discovery and readiness-style use cases. The returned descriptors are
     * metadata only; callers must not infer that an implementation can be invoked without also
     * checking the executor and implementation registries used by the runtime.
     */
    fun all(): List<ToolDescriptor>
}

/**
 * Lightweight in-memory [ToolRegistry].
 *
 * This implementation is useful for tests, examples, local runtimes, and simple embedded setups
 * where all tool descriptors are assembled in code. More advanced runtimes can replace it with a
 * persistence-backed or configuration-backed registry without changing the execution contract.
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
     *
     * Replacement is intentional for in-memory composition: later registration wins for the same
     * tool code. The registry only stores descriptors and does not automatically register an
     * executor or implementation.
     */
    fun register(descriptor: ToolDescriptor) {
        descriptorsByCode[descriptor.code] = descriptor
    }

    override fun find(code: ToolCode): ToolDescriptor? = descriptorsByCode[code]

    override fun all(): List<ToolDescriptor> = descriptorsByCode.values.toList()
}
