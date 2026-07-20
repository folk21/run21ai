package ai.run21.core.dispatch

import ai.run21.core.model.DispatcherChainResult
import ai.run21.core.model.DispatchInput
import ai.run21.core.model.ToolError
import ai.run21.core.model.ToolErrorCode

/**
 * Dispatcher chain used when a requested chain profile is unavailable.
 *
 * This class is an unsupported default, similar in purpose to `UnsupportedToolExecutor`. It lets a
 * runtime return a normal structured dispatch rejection instead of throwing when a named dispatcher
 * chain cannot be resolved.
 *
 * The chain does not inspect or transform input. Every call returns `DISPATCHER_NOT_REGISTERED`
 * with the input's existing trace.
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
