package ai.run21.core.dispatch

import ai.run21.core.model.DispatchContext
import ai.run21.core.model.DispatchInput
import ai.run21.core.model.DispatcherCode
import ai.run21.core.model.DispatcherOutcome

/**
 * Single step in the dispatcher chain.
 *
 * A dispatcher may pass, enrich input, reject execution, or accept input by producing a
 * structured ToolRequest. Dispatchers never perform real tool work.
 */
interface Dispatcher {
    val code: DispatcherCode

    suspend fun dispatch(input: DispatchInput, context: DispatchContext): DispatcherOutcome
}
