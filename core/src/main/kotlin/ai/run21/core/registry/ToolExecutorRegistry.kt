package ai.run21.core.registry

import ai.run21.core.execution.ToolExecutor
import ai.run21.core.model.ToolCode

/**
 * Registry that resolves the executor responsible for a tool.
 *
 * `ToolExecutorRegistry` maps a [ToolCode] to a [ToolExecutor]. The executor is the tool-level
 * execution coordinator: it validates a `ToolRequest`, resolves the selected
 * `ToolImplementation`, invokes it, and turns the output or error into a canonical `ToolResult`.
 *
 * This registry is intentionally separate from `ToolRegistry`. A `ToolRegistry` answers
 * "is this tool capability declared and what is its contract?" while this registry answers
 * "which executor handles requests for this tool?".
 *
 * This registry also does not choose among multiple concrete implementations. That responsibility
 * belongs inside the resolved `ToolExecutor`, usually through a `ToolImplementationRegistry`.
 */
interface ToolExecutorRegistry {
    /**
     * Returns the executor responsible for the target tool.
     *
     * A missing executor means the tool may be declared but is not executable in the current
     * runtime. The default core execution service turns that case into a structured unsupported
     * tool result.
     */
    fun find(toolCode: ToolCode): ToolExecutor?

    /**
     * Returns all registered executors in registry order.
     *
     * This is mostly useful for diagnostics, tests, and runtime composition checks. Discovery APIs
     * should normally expose `ToolRegistry` descriptors rather than executors.
     */
    fun all(): List<ToolExecutor>
}

/**
 * Lightweight in-memory [ToolExecutorRegistry].
 *
 * This implementation is useful when a runtime assembles executors in code. It keeps one executor
 * per tool code and preserves registration order for `all()`.
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
     *
     * Replacement is intentional for in-memory composition: later registration wins for the same
     * tool code. Registering an executor here does not register a tool descriptor in
     * `ToolRegistry`; both registries must be wired consistently by the runtime.
     */
    fun register(executor: ToolExecutor) {
        executorsByToolCode[executor.toolCode] = executor
    }

    override fun find(toolCode: ToolCode): ToolExecutor? = executorsByToolCode[toolCode]

    override fun all(): List<ToolExecutor> = executorsByToolCode.values.toList()
}
