package ai.run21.core.registry

import ai.run21.core.execution.ToolExecutor
import ai.run21.core.model.ToolCode

/**
 * Registry that resolves tool executors by target tool code.
 */
interface ToolExecutorRegistry {
    /**
     * Returns the executor responsible for the target tool.
     */
    fun find(toolCode: ToolCode): ToolExecutor?

    /**
     * Returns all registered executors in registry order.
     */
    fun all(): List<ToolExecutor>
}

/**
 * Lightweight in-memory executor registry.
 */
class InMemoryToolExecutorRegistry(
    executors: Iterable<ToolExecutor> = emptyList(),
) : ToolExecutorRegistry {
    private val executorsByToolCode: MutableMap<ToolCode, ToolExecutor> = linkedMapOf()

    init {
        executors.forEach(::register)
    }

    /**
     * Registers or replaces an executor for its declared tool code.
     */
    fun register(executor: ToolExecutor) {
        executorsByToolCode[executor.toolCode] = executor
    }

    override fun find(toolCode: ToolCode): ToolExecutor? = executorsByToolCode[toolCode]

    override fun all(): List<ToolExecutor> = executorsByToolCode.values.toList()
}
