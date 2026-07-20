package ai.run21.core.dispatch

import ai.run21.core.model.DispatcherChainResult
import ai.run21.core.model.DispatchInput

/**
 * Pipeline that turns caller input into a tool request or a structured rejection.
 *
 * A dispatcher chain is the required boundary before tool execution. Structured calls, raw prompts,
 * and custom input types all enter through a chain. For direct structured calls, the chain can be a
 * simple pass-through chain. For raw or prompt-like calls, the chain may contain rule-based,
 * validation, enrichment, custom, or LLM-backed dispatchers.
 *
 * The chain exists so a runtime can compose several dispatch strategies without creating separate
 * execution paths. Each dispatcher either accepts the input as a `ToolRequest`, rejects it, enriches
 * it for later dispatchers, or passes so the next dispatcher can try.
 *
 * A successful chain result does not execute the tool. It only produces the canonical request that
 * the execution service will pass to the resolved `ToolExecutor`.
 */
interface DispatcherChain {
    /**
     * Runs the chain for one input and returns either an accepted request or a structured dispatch
     * rejection, including dispatcher trace metadata.
     */
    suspend fun dispatch(input: DispatchInput): DispatcherChainResult
}
