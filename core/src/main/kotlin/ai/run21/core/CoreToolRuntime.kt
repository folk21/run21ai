package ai.run21.core

import ai.run21.core.dispatch.DispatcherChain
import ai.run21.core.execution.DefaultToolExecutionService
import ai.run21.core.execution.RawToolCallBuilder
import ai.run21.core.execution.ToolCallBuilder
import ai.run21.core.execution.ToolCalls
import ai.run21.core.execution.ToolExecutor
import ai.run21.core.execution.ToolExecutionService
import ai.run21.core.model.DispatchInput
import ai.run21.core.model.ToolDescriptor
import ai.run21.core.model.ToolResult
import ai.run21.core.registry.InMemoryToolExecutorRegistry
import ai.run21.core.registry.InMemoryToolRegistry

/**
 * Tool descriptors and executors that can be installed into a core runtime.
 *
 * Tool modules can expose this value so runtime modules do not need to know how a tool descriptor,
 * implementation registry, executor, and validator are wired internally.
 */
data class CoreToolRegistration(
    val tools: List<ToolDescriptor>,
    val executors: List<ToolExecutor>,
)

/**
 * In-memory core runtime assembly created by [CoreToolRuntime.inMemoryAssembly].
 *
 * Some runtimes need more than the facade itself. For example, an HTTP runtime may execute through
 * [runtime], resolve requests through [dispatcherChain], and expose tool discovery from
 * [toolRegistry]. This value keeps those shared objects together so callers do not need to rebuild
 * equivalent registries in several places.
 */
data class CoreToolRuntimeAssembly(
    val runtime: CoreToolRuntime,
    val dispatcherChain: DispatcherChain,
    val toolRegistry: InMemoryToolRegistry,
    val executorRegistry: InMemoryToolExecutorRegistry,
)

/**
 * Root-level facade for calling a configured core tool execution pipeline.
 *
 * `CoreToolRuntime` is intentionally small: it gives callers a visible entry point directly under
 * the `ai.run21.core` package while preserving the core execution model. The facade does not select
 * tools, resolve implementations, run dispatchers itself, or create a second execution path.
 *
 * All calls are delegated to the supplied [ToolExecutionService]. Structured and raw builder-based
 * calls additionally go through [ToolCalls], so they keep the same behavior as direct use of the
 * recommended high-level API:
 *
 * - structured calls are wrapped as `StructuredDispatchInput`;
 * - raw calls are built as `RawDispatchInput`;
 * - every call still enters the configured dispatcher chain before tool execution;
 * - the resolved `ToolExecutor` and `ToolImplementation` remain responsible for real work.
 *
 * Runtime modules can wrap this facade with application-specific concerns such as HTTP, CLI, or
 * local in-memory profiles. Those concerns should stay outside `core`.
 */
class CoreToolRuntime(
    /**
     * The configured execution boundary used by this facade.
     *
     * Keeping the service visible lets advanced callers inspect or pass the underlying core service
     * to lower-level APIs without duplicating runtime wiring.
     */
    val executionService: ToolExecutionService,
) {
    /**
     * Executes already-built dispatch input through the configured dispatcher chain.
     *
     * Use this overload when the caller already has a [DispatchInput], for example a
     * `StructuredDispatchInput` produced elsewhere or a raw input object built without the DSL.
     */
    suspend fun execute(input: DispatchInput): ToolResult =
        executionService.execute(input)

    /**
     * Builds and executes a structured tool request.
     *
     * This is the facade equivalent of [ToolCalls.execute]. The builder creates a `ToolRequest`,
     * `ToolCalls` wraps it as structured dispatch input, and [executionService] runs the normal
     * dispatcher-chain-to-executor pipeline.
     */
    suspend fun execute(block: ToolCallBuilder.() -> Unit): ToolResult =
        ToolCalls.execute(executionService, block)

    /**
     * Builds and dispatches raw or prompt-like input.
     *
     * This is the facade equivalent of [ToolCalls.dispatch]. The configured dispatcher chain decides
     * whether the input can be normalized into a `ToolRequest` or should be rejected with a
     * structured error.
     */
    suspend fun dispatch(block: RawToolCallBuilder.() -> Unit): ToolResult =
        ToolCalls.dispatch(executionService, block)

    companion object {
        /**
         * Creates a [CoreToolRuntime] backed by the standard in-memory core implementation.
         *
         * This helper centralizes the common lightweight wiring used by tests, examples, local
         * runtimes, and simple embedded deployments:
         *
         * - [dispatcherChain] defines how input becomes a `ToolRequest`;
         * - [tools] are registered in an [InMemoryToolRegistry];
         * - [executors] are registered in an [InMemoryToolExecutorRegistry];
         * - [DefaultToolExecutionService] connects those pieces into the core execution pipeline;
         * - the resulting service is exposed through [CoreToolRuntime].
         *
         * The default dispatcher chain is the structured direct profile, which uses the
         * pass-through dispatcher. Callers that support raw or prompt-like input should pass an
         * explicit dispatcher chain that contains their normalizers or LLM dispatchers.
         */
        fun inMemory(
            tools: Iterable<ToolDescriptor>,
            executors: Iterable<ToolExecutor>,
            dispatcherChain: DispatcherChain = CoreDefaults.structuredDirectDispatcherChain(),
        ): CoreToolRuntime =
            inMemoryAssembly(
                tools = tools,
                executors = executors,
                dispatcherChain = dispatcherChain,
            ).runtime

        /**
         * Creates a [CoreToolRuntime] from a tool module registration.
         */
        fun inMemory(
            registration: CoreToolRegistration,
            dispatcherChain: DispatcherChain = CoreDefaults.structuredDirectDispatcherChain(),
        ): CoreToolRuntime =
            inMemory(
                tools = registration.tools,
                executors = registration.executors,
                dispatcherChain = dispatcherChain,
            )

        /**
         * Creates the standard in-memory core runtime and returns its reusable assembled parts.
         *
         * Use this when a runtime needs direct access to the dispatcher chain or registries in
         * addition to the [CoreToolRuntime] facade. The returned objects are the exact instances
         * used by the execution service, so discovery, request resolution, readiness checks, and
         * execution all observe the same in-memory registration state.
         */
        fun inMemoryAssembly(
            tools: Iterable<ToolDescriptor>,
            executors: Iterable<ToolExecutor>,
            dispatcherChain: DispatcherChain = CoreDefaults.structuredDirectDispatcherChain(),
        ): CoreToolRuntimeAssembly {
            val toolRegistry = InMemoryToolRegistry(tools)
            val executorRegistry = InMemoryToolExecutorRegistry(executors)
            val service = DefaultToolExecutionService(
                dispatcherChain = dispatcherChain,
                toolRegistry = toolRegistry,
                executorRegistry = executorRegistry,
            )

            return CoreToolRuntimeAssembly(
                runtime = CoreToolRuntime(service),
                dispatcherChain = dispatcherChain,
                toolRegistry = toolRegistry,
                executorRegistry = executorRegistry,
            )
        }

        /**
         * Creates the standard in-memory core runtime assembly from a tool module registration.
         */
        fun inMemoryAssembly(
            registration: CoreToolRegistration,
            dispatcherChain: DispatcherChain = CoreDefaults.structuredDirectDispatcherChain(),
        ): CoreToolRuntimeAssembly =
            inMemoryAssembly(
                tools = registration.tools,
                executors = registration.executors,
                dispatcherChain = dispatcherChain,
            )
    }
}
