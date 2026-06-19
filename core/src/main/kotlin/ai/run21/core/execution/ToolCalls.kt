package ai.run21.core.execution

import ai.run21.core.model.RawDispatchInput
import ai.run21.core.model.StructuredDispatchInput
import ai.run21.core.model.ToolRequest
import ai.run21.core.model.ToolResult

/**
 * Convenience entry points for direct structured tool calls.
 *
 * The object keeps call sites compact while preserving the core execution model: the builder creates
 * a [ToolRequest], the request is wrapped into [StructuredDispatchInput], and execution still runs
 * through the configured dispatcher chain.
 */
object ToolCalls {
    /**
     * Builds a [RawDispatchInput] using [RawToolCallBuilder].
     */
    fun rawInput(block: RawToolCallBuilder.() -> Unit): RawDispatchInput =
        RawToolCallBuilder()
            .apply(block)
            .build()

    /**
     * Builds a [ToolRequest] using [ToolCallBuilder].
     */
    fun request(block: ToolCallBuilder.() -> Unit): ToolRequest =
        ToolCallBuilder()
            .apply(block)
            .build()

    /**
     * Builds and executes a direct structured tool request.
     */
    suspend fun execute(
        service: ToolExecutionService,
        block: ToolCallBuilder.() -> Unit,
    ): ToolResult = service.execute(
        StructuredDispatchInput(request(block)),
    )

    /**
     * Builds and executes raw input through the configured dispatcher chain.
     *
     * This entry point is useful for rule-based, custom, or LLM-style dispatchers that receive
     * prompt-like input and produce a canonical ToolRequest.
     */
    suspend fun dispatch(
        service: ToolExecutionService,
        block: RawToolCallBuilder.() -> Unit,
    ): ToolResult = service.execute(rawInput(block))
}
