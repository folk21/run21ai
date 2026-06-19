package ai.run21.core.dispatch

import ai.run21.core.model.DispatcherChainResult
import ai.run21.core.model.DispatchInput
import ai.run21.core.model.ToolError
import ai.run21.core.model.ToolErrorCode

/**
 * Dispatcher chain implementation used when a requested chain is unavailable. It returns a
 * structured rejection with DISPATCHER_NOT_REGISTERED.
 */
class UnsupportedDispatcherChain : DispatcherChain {
    override suspend fun dispatch(input: DispatchInput): DispatcherChainResult =
        DispatcherChainResult.rejected(
            error = ToolError(
                code = ToolErrorCode.DISPATCHER_NOT_REGISTERED,
                message = "Dispatcher chain is not registered.",
            ),
            trace = input.trace,
        )
}
