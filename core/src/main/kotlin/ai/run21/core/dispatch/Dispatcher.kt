package ai.run21.core.dispatch

import ai.run21.core.model.DispatchContext
import ai.run21.core.model.DispatchInput
import ai.run21.core.model.DispatcherCode
import ai.run21.core.model.DispatcherOutcome

/**
 * Single input-normalization step in a [DispatcherChain].
 *
 * A dispatcher receives a [DispatchInput] and decides whether it can move execution closer to a
 * canonical `ToolRequest`. The word "dispatch" means "route or normalize input into a tool
 * request", not "execute the tool".
 *
 * A dispatcher can produce four kinds of outcome:
 *
 * - accept: the input has been converted into a `ToolRequest`;
 * - reject: execution should stop with a structured dispatch error;
 * - enrich: the input has been improved or normalized and should continue to the next dispatcher;
 * - pass: this dispatcher cannot handle the input, so the next dispatcher may try.
 *
 * Dispatchers are allowed to interpret raw input, classify intent, select a target tool, prepare
 * request payloads, add metadata, or reject ambiguous input. They must not perform real tool work,
 * call a `ToolImplementation`, or bypass the executor layer.
 */
interface Dispatcher {
    /**
     * Stable dispatcher identifier used in dispatcher traces and diagnostics.
     */
    val code: DispatcherCode

    /**
     * Attempts to dispatch the current input.
     *
     * [input] is the latest input state. It may be the original caller input or an enriched value
     * produced by an earlier dispatcher in the same chain. [context] carries shared correlation and
     * attributes for the whole chain run.
     */
    suspend fun dispatch(input: DispatchInput, context: DispatchContext): DispatcherOutcome
}
